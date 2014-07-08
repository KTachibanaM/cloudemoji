package org.ktachibana.cloudemoji.activities;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.orm.SugarApp;
import com.orm.query.Condition;
import com.orm.query.Select;

import org.apache.commons.io.IOUtils;
import org.ktachibana.cloudemoji.BaseActivity;
import org.ktachibana.cloudemoji.Constants;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.events.CategoryClickedEvent;
import org.ktachibana.cloudemoji.events.EntryCopiedAndAddedToHistoryEvent;
import org.ktachibana.cloudemoji.events.FavoriteAddedEvent;
import org.ktachibana.cloudemoji.events.FavoriteDeletedEvent;
import org.ktachibana.cloudemoji.events.LocalRepositoryClickedEvent;
import org.ktachibana.cloudemoji.events.RemoteRepositoryClickedEvent;
import org.ktachibana.cloudemoji.events.RemoteRepositoryParsedEvent;
import org.ktachibana.cloudemoji.events.SecondaryMenuItemClickedEvent;
import org.ktachibana.cloudemoji.fragments.FavoriteFragment;
import org.ktachibana.cloudemoji.fragments.HistoryFragment;
import org.ktachibana.cloudemoji.fragments.LeftDrawerFragment;
import org.ktachibana.cloudemoji.fragments.SourceFragment;
import org.ktachibana.cloudemoji.models.Favorite;
import org.ktachibana.cloudemoji.models.Repository;
import org.ktachibana.cloudemoji.models.Source;
import org.ktachibana.cloudemoji.utils.NotificationHelper;
import org.ktachibana.cloudemoji.utils.ParcelableObjectInMemoryCache;
import org.ktachibana.cloudemoji.utils.SourceJsonParser;
import org.ktachibana.cloudemoji.utils.SourceXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity implements
        Constants,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final long DEFAULT_REPOSITORY_ID = LIST_ITEM_FAVORITE_ID;
    private static final String CURRENT_REPOSITORY_ID_TAG = "currentRepositoryId";
    private static final String CURRENT_REPOSITORY_SOURCE_TAG = "currentRepositorySource";
    private static final String CURRENT_SOURCE_CACHE_TAG = "currentSourceCache";
    // Views
    @Optional // Optional because on split view it doesn't exist
    @InjectView(R.id.drawerLayout)
    DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle toggle;
    // State
    private long mCurrentRepositoryId;
    private Source mCurrentSource;
    private ParcelableObjectInMemoryCache<Source> mCurrentSourceCache;
    private SourceFragment mCurrentSourceFragment;
    // etc
    private SharedPreferences mPreferences;
    private boolean mIsDrawerStatic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        EventBus.getDefault().register(this);

        // Choose layout to inflate
        setupLayout();

        // Setup views
        setupViews();

        // Toggle notification state
        switchNotificationState();

        // Check first time run
        firstTimeCheck();

        // If not starting from refresh new, get state
        if (savedInstanceState != null) {
            mCurrentRepositoryId = savedInstanceState.getLong(CURRENT_REPOSITORY_ID_TAG);
            mCurrentSource = savedInstanceState.getParcelable(CURRENT_REPOSITORY_SOURCE_TAG);
            mCurrentSourceCache = savedInstanceState.getParcelable(CURRENT_SOURCE_CACHE_TAG);
        }

        // Else, set it to display default
        else {
            mCurrentRepositoryId = DEFAULT_REPOSITORY_ID;
            mCurrentSource = null;
            mCurrentSourceCache = new ParcelableObjectInMemoryCache<Source>();
        }

        // Setup left drawer with repository id and source
        setupLeftDrawer(mCurrentRepositoryId, mCurrentSource);

        // Switch to the repository
        internalSwitchRepository();

    }

    private void setupLayout() {
        // Set up UI layout according to user preference for drawer and split view
        String uiPreference = mPreferences.getString(PREF_SPLIT_VIEW, "auto");
        int orientation = getResources().getConfiguration().orientation;

        // If auto, set up the default layout optimized for landscape and tablets
        if (uiPreference.equals("auto")) {
            setContentView(R.layout.activity_main);
        }

        // If split_in_port, only manually set up split view if detected portrait
        else if (uiPreference.equals("split_in_port")) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                setContentView(R.layout.activity_main_manual_split_view);
            } else {
                setContentView(R.layout.activity_main_manual_navigation_drawer);
            }
        }

        // If split_in_land, only manually set up split view if detected landscape
        else if (uiPreference.equals("split_in_land")) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setContentView(R.layout.activity_main_manual_split_view);
            } else {
                setContentView((R.layout.activity_main_manual_navigation_drawer));
            }
        }

        // Else split_in_both, manually set up split view for both orientations
        else {
            setContentView(R.layout.activity_main_manual_split_view);
        }
    }

    private void setupViews() {
        ButterKnife.inject(this);

        // If mDrawerLayout not found, then the drawer is static
        mIsDrawerStatic = (mDrawerLayout == null);

        // Set up toggle
        if (!mIsDrawerStatic) {
            toggle = new ActionBarDrawerToggle(
                    this,
                    mDrawerLayout,
                    R.drawable.ic_ab_navigation_drawer,
                    R.string.app_name,
                    R.string.app_name);
            mDrawerLayout.setDrawerListener(toggle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void firstTimeCheck() {
        boolean hasRunBefore = mPreferences.getBoolean(PREF_HAS_RUN_BEFORE, false);

        upgradeFavoriteDatabaseIfExists();
        setupDefaultRepoIfNotExists();

        // If hasn't run before
        if (!hasRunBefore) {
            // It has run
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(PREF_HAS_RUN_BEFORE, true);
            editor.commit();
        }
    }

    @SuppressWarnings("unchecked")
    private void setupDefaultRepoIfNotExists() {
        // Find repository with default url
        List<Repository> kt = Select
                .from(Repository.class)
                .where(Condition.prop("url")
                        .eq(DEFAULT_REPOSITORY_URL))
                .list();
        if (kt.size() != 0) {
            // If found, ignore below
            return;
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // Save record to database
            Repository defaultRepository = new Repository(this, DEFAULT_REPOSITORY_URL, "KT");
            defaultRepository.save();

            // Load file from assets and save to file system
            inputStream = getAssets().open("test.xml");
            File file = new File(
                    SugarApp.getSugarContext().getFilesDir(), defaultRepository.getFileName());
            outputStream = new FileOutputStream(file);

            // Copying
            IOUtils.copy(inputStream, outputStream);

            // Set available to true and SAVE
            defaultRepository.setAvailable(true);
            defaultRepository.save();
        } catch (FileNotFoundException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void upgradeFavoriteDatabaseIfExists() {
        // Find old database file
        File oldDatabaseFile = getDatabasePath("mydb.db");
        if (!oldDatabaseFile.exists()) {
            // If file does not exist, ignore below
            return;
        }

        try {
            // Read the old favorite database and table cursor
            SQLiteDatabase oldDatabase
                    = SQLiteDatabase.openDatabase(oldDatabaseFile.getPath(), null, 0);
            Cursor cursor = oldDatabase.query(
                    "favorites",                    // table name
                    new String[]{"string", "note"}, // columns
                    null, null, null, null, null
            );

            // Read all favorites
            List<Favorite> favorites = new ArrayList<Favorite>();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String emoticon = cursor.getString(0);
                String description = cursor.getString(1);
                favorites.add(new Favorite(this, emoticon, description));
                cursor.moveToNext();
            }
            cursor.close();

            // SAVE
            for (Favorite favorite : favorites) {
                favorite.save();
            }

            // Remove the database
            SQLiteDatabase.deleteDatabase(oldDatabaseFile);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private void setupLeftDrawer(long repositoryId, Source source) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.leftDrawer, LeftDrawerFragment.newInstance(repositoryId, source))
                .commit();
    }

    private void switchNotificationState() {
        NotificationHelper
                .switchNotificationState(this,
                        mPreferences.getString(PREF_NOTIFICATION_VISIBILITY, "both"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key) {
        if (PREF_NOTIFICATION_VISIBILITY.equals(key)) {
            switchNotificationState();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (!mIsDrawerStatic) {
            if (toggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!mIsDrawerStatic) {
            toggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mIsDrawerStatic) {
            toggle.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Listens for any string copied and send it to clipboard
     *
     * @param event string copied event
     */
    @SuppressWarnings("deprecation")
    public void onEvent(EntryCopiedAndAddedToHistoryEvent event) {
        String copied = event.getEntry().getEmoticon();

        int SDK = Build.VERSION.SDK_INT;
        // Below 3.0
        if (SDK < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard
                    = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(copied);
        }

        // Above 3.0
        else {
            android.content.ClipboardManager clipboard
                    = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("emoji", copied);
            clipboard.setPrimaryClip(clip);
        }

        // Show toast
        Toast.makeText(MainActivity.this, copied + "\n" + getString(R.string.copied), Toast.LENGTH_SHORT).show();
        boolean isCloseAfterCopy = mPreferences.getBoolean(PREF_CLOSE_AFTER_COPY, true);

        // Close if you want
        if (isCloseAfterCopy) {
            finish();
        }
    }

    public void onEvent(LocalRepositoryClickedEvent event) {
        mCurrentRepositoryId = event.getId();
        mCurrentSource = null;
        internalSwitchRepository();
    }

    public void onEvent(RemoteRepositoryClickedEvent event) {
        mCurrentRepositoryId = event.getId();
        mCurrentSource = readSourceFromFile(event.getId());
        internalSwitchRepository();
    }

    public void onEvent(CategoryClickedEvent event) {
        // ni kan kan ni, you see see you
        if (mCurrentSourceFragment != null) {
            mCurrentSourceFragment.setSelection(event.getIndex());
            closeDrawers();
        }
    }

    public void onEvent(FavoriteAddedEvent event) {
        Toast.makeText(
                this,
                event.getEmoticon() + "\n" + getString(R.string.added_to_fav),
                Toast.LENGTH_SHORT
        ).show();
    }

    public void onEvent(FavoriteDeletedEvent event) {
        Toast.makeText(
                this,
                event.getEmoticon() + "\n" + getString(R.string.removed_from_fav),
                Toast.LENGTH_SHORT
        ).show();
    }

    public void onEvent(SecondaryMenuItemClickedEvent event) {
        final long id = event.getId();
        if (id == LIST_ITEM_REPOSITORY_MANAGER_ID) {
            Intent intent = new Intent(this, RepositoryManagerActivity.class);
            startActivityForResult(intent, REPOSITORY_MANAGER_REQUEST_CODE);
        } else if (id == LIST_ITEM_SETTINGS_ID) {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        } else if (id == LIST_ITEM_EXIT_ID) {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(PERSISTENT_NOTIFICATION_ID);
            finish();
        }
    }

    private void internalSwitchRepository() {
        // If it is a local repository
        if (mCurrentRepositoryId < 0) {
            // Nullify source fragment
            mCurrentSourceFragment = null;

            // Switch to correct fragment
            if (mCurrentRepositoryId == LIST_ITEM_FAVORITE_ID)
                replaceMainContainer(new FavoriteFragment());
            if (mCurrentRepositoryId == LIST_ITEM_HISTORY_ID)
                replaceMainContainer(new HistoryFragment());

            // Close drawers
            closeDrawers();
        }

        // Else it is a remote repository with a parsed source
        else {
            // Create source fragment and switch, notify left drawer
            if (mCurrentSource != null) {
                mCurrentSourceFragment = SourceFragment.newInstance(mCurrentSource);
                replaceMainContainer(mCurrentSourceFragment);
                EventBus.getDefault().post(
                        new RemoteRepositoryParsedEvent(mCurrentRepositoryId, mCurrentSource));
            }

            // Do not close drawers
        }
    }

    private void replaceMainContainer(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainContainer, fragment)
                .commit();
    }

    private void closeDrawers() {
        if (!mIsDrawerStatic) mDrawerLayout.closeDrawers();
    }

    private Source readSourceFromFile(long id) {
        // Try to retrieve Source object from cache
        if (mCurrentSourceCache.contains(id) != null) {
            return mCurrentSourceCache.get(id);
        }

        Source source = null;

        // Get repository file name
        final Repository repository = Repository.findById(Repository.class, id);
        String fileName = repository.getFileName();

        // Read the file from file system
        File file = new File(SugarApp.getSugarContext().getFilesDir(), fileName);

        // Read it
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
            try {
                Repository.FormatType formatType = repository.getFormatType();

                // Parse source from file
                if (formatType == Repository.FormatType.XML) {
                    source = new SourceXmlParser().parse(fileReader);
                } else if (formatType == Repository.FormatType.JSON) {
                    source = new SourceJsonParser().parse(IOUtils.toString(fileReader));
                }

                // Put parsed Source to cache
                mCurrentSourceCache.put(id, source);
            }

            // Parser error
            catch (XmlPullParserException e) {
                Toast.makeText(this, getString(R.string.invalid_repo_format), Toast.LENGTH_SHORT)
                        .show();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, e.getLocalizedMessage());
            } catch (Exception e) {
                Log.e(DEBUG_TAG, e.getLocalizedMessage());
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            IOUtils.closeQuietly(fileReader);
        }

        return source;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REPOSITORY_MANAGER_REQUEST_CODE) {
            /**
             * If coming back from repository manager, the current id may not be valid
             * So we want to check for that and set it to default if it is invalid
             */
            if (mCurrentRepositoryId >= 0) {
                if (Repository.findById(Repository.class, mCurrentRepositoryId) == null) {
                    mCurrentRepositoryId = DEFAULT_REPOSITORY_ID;
                    mCurrentSource = null;
                }
            }

            // Setup left drawer with repository id and source
            setupLeftDrawer(mCurrentRepositoryId, mCurrentSource);

            // Switch to the repository
            internalSwitchRepository();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save current state
        outState.putLong(CURRENT_REPOSITORY_ID_TAG, mCurrentRepositoryId);
        outState.putParcelable(CURRENT_REPOSITORY_SOURCE_TAG, mCurrentSource);
        outState.putParcelable(CURRENT_SOURCE_CACHE_TAG, mCurrentSourceCache);
    }
}