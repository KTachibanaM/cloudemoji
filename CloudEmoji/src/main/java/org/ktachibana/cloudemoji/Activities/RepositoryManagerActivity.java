package org.ktachibana.cloudemoji.activities;

import android.os.Bundle;
import android.view.MenuItem;

import org.ktachibana.cloudemoji.BaseActivity;
import org.ktachibana.cloudemoji.Constants;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.fragments.RepositoryFragment;


public class RepositoryManagerActivity extends BaseActivity implements Constants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repository_manager);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.repositoryMainContainer, new RepositoryFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                setResult(REPOSITORY_MANAGER_REQUEST_CODE);
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
