/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.ShowcaseHelper;
import com.mainmethod.premofm.ui.activity.ChannelCategoryActivity;
import com.mainmethod.premofm.ui.activity.ChannelSearchActivity;
import com.mainmethod.premofm.ui.activity.PremoActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evan on 12/3/14.
 */
public class ExploreFragment extends BaseFragment implements ViewPager.OnPageChangeListener {

    public static final int EXPLORE_TRENDING_CHANNELS = 1;
    public static final int EXPLORE_TOP_CHANNELS = 2;
    public static final int EXPLORE_TOP_COLLECTIONS = 3;
    public static final int EXPLORE_TRENDING_EPISODES = 4;

    private ViewPager mViewPager;
    private Adapter mAdapter;

    /**
     * Creates a new instance of this fragment
     * @return
     */
    public static ExploreFragment newInstance() {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  super.onCreateView(inflater, container, savedInstanceState);
        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        mViewPager.addOnPageChangeListener(this);
        setupViewPager();
        return view;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_explore;
    }

    @Override
    protected int getFragmentTitleResourceId() {
        return R.string.title_fragment_explore;
    }

    @Override
    public boolean hasTabs() {
        return true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public int getMenuResourceId() {
        return R.menu.menu_explore_fragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                ChannelSearchActivity.start(getActivity());
                return true;
            case R.id.action_category:
                ChannelCategoryActivity.start(getActivity());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        ShowcaseHelper.showCategoryShowcase(getBaseActivity());
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void setupViewPager() {
        mAdapter = new Adapter(getChildFragmentManager());
        mAdapter.addFragment(ExploreFragmentPage.newInstance(EXPLORE_TOP_CHANNELS),
                getString(R.string.explore_fragment_type_top_channels));
        mAdapter.addFragment(ExploreFragmentPage.newInstance(EXPLORE_TRENDING_CHANNELS),
                getString(R.string.explore_fragment_type_trending_channels));
        /*mAdapter.addFragment(ExploreFragmentPage.newInstance(EXPLORE_TOP_COLLECTIONS),
                getString(R.string.explore_fragment_type_top_collections));*/
        mAdapter.addFragment(ExploreFragmentPage.newInstance(EXPLORE_TRENDING_EPISODES),
                getString(R.string.trending_episodes));
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(0);
        ((PremoActivity) getActivity()).setViewPager(mViewPager);
    }

    static class Adapter extends FragmentStatePagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}