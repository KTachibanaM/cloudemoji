package org.ktachibana.cloudemoji.activities;

import android.os.Bundle;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.ktachibana.cloudemoji.BaseActivity;
import org.ktachibana.cloudemoji.BaseHttpClient;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.adapters.RepositoryStoreListViewAdapter;
import org.ktachibana.cloudemoji.events.RepositoryAddedEvent;
import org.ktachibana.cloudemoji.events.RepositoryDuplicatedEvent;
import org.ktachibana.cloudemoji.models.inmemory.StoreRepository;
import org.ktachibana.cloudemoji.net.RepositoryStoreDownloaderClient;
import org.ktachibana.cloudemoji.utils.UncancelableProgressMaterialDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class RepositoryStoreActivity extends BaseActivity {
    private static final String STATE_TAG = "state";
    private List<StoreRepository> mRepositories;

    @InjectView(R.id.list)
    ListView mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repository_store);

        ButterKnife.inject(this);
        EventBus.getDefault().register(this);

        if (savedInstanceState != null) {
            mRepositories = savedInstanceState.getParcelableArrayList(STATE_TAG);
            showRepositoryStore();
            return;
        }

        final MaterialDialog dialog = new UncancelableProgressMaterialDialogBuilder(this)
                .title(R.string.please_wait)
                .content(R.string.downloading)
                .show();

        new RepositoryStoreDownloaderClient().downloadRepositoryStore(new BaseHttpClient.ListCallback() {
            @Override
            public void success(List result) {
                mRepositories = result;
                showRepositoryStore();
                dialog.dismiss();
            }

            @Override
            public void fail(Throwable t) {
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(STATE_TAG, (ArrayList<StoreRepository>) mRepositories);
    }

    private void showRepositoryStore() {
        mList.setAdapter(new RepositoryStoreListViewAdapter(this, mRepositories));
    }

    public void onEvent(RepositoryAddedEvent event) {
        showSnackBar(event.getRepository().getAlias());
    }

    public void onEvent(RepositoryDuplicatedEvent event) {
        showSnackBar(R.string.duplicate_url);
    }
}
