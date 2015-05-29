package org.ktachibana.cloudemoji.fragments;


import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import org.ktachibana.cloudemoji.BaseFragment;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.adapters.EmojiconsPagerAdapter;
import org.ktachibana.cloudemoji.utils.SourceInMemoryCache;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class RepositoriesFragment extends BaseFragment {
    private static final String ARG_CACHE = "cache";
    @InjectView(R.id.pager)
    ViewPager mPager;
    @InjectView(R.id.tabs)
    PagerSlidingTabStrip mTabs;

    private SourceInMemoryCache mCache;

    public static RepositoriesFragment newInstance(SourceInMemoryCache cache) {
        RepositoriesFragment fragment = new RepositoriesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CACHE, cache);
        fragment.setArguments(args);
        return fragment;
    }

    public RepositoriesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCache = getArguments().getParcelable(ARG_CACHE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Setup views
        View rootView = inflater.inflate(R.layout.fragment_tab_and_pager, container, false);
        ButterKnife.inject(this, rootView);

        // Setup contents
        mPager.setAdapter(new EmojiconsPagerAdapter(getChildFragmentManager()));
        mTabs.setViewPager(mPager);

        return rootView;
    }


}
