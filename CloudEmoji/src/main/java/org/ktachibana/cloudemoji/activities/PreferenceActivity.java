package org.ktachibana.cloudemoji.activities;

import android.os.Bundle;
import android.view.MenuItem;

import org.ktachibana.cloudemoji.BaseActivity;
import org.ktachibana.cloudemoji.Constants;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.fragments.PreferenceFragment;

public class PreferenceActivity extends BaseActivity implements Constants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainContainer, new PreferenceFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(PREFERENCE_REQUEST_CODE);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}