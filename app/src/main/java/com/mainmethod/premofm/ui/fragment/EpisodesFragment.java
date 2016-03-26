/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.ShowcaseHelper;
import com.mainmethod.premofm.object.Filter;
import com.mainmethod.premofm.ui.activity.EditFilterActivity;
import com.mainmethod.premofm.ui.activity.PremoActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evan on 12/3/14.
 */
public class EpisodesFragment extends BaseFragment implements
        DialogInterface.OnClickListener,
        ViewPager.OnPageChangeListener {

    private static final String PARAM_CURRENT_FILTER = "CurrentFilter";

    private ViewPager mViewPager;
    private Adapter mAdapter;
    private List<Filter> mFilters;
    private boolean mCurrentTabPressedOnce = false;

    /**
     * Creates a new instance of this fragment
     * @return
     */
    public static EpisodesFragment newInstance(Bundle args) {
        EpisodesFragment fragment = new EpisodesFragment();

        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public boolean hasTabs() {
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        mCurrentTabPressedOnce = true;

        if (savedInstanceState != null && savedInstanceState.containsKey(PARAM_CURRENT_FILTER)) {
            setupViewPager(savedInstanceState.getInt(PARAM_CURRENT_FILTER));
        } else {
            setupViewPager(0);
        }
        return view;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_episodes;
    }

    @Override
    protected int getFragmentTitleResourceId() {
        return R.string.title_fragment_home;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.menu_home_fragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                showFilterDialog(null);
                AnalyticsHelper.sendEvent(getActivity(),
                        AnalyticsHelper.CATEGORY_CREATE_FILTER,
                        AnalyticsHelper.ACTION_CLICK,
                        null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        ShowcaseHelper.showFilterShowcase(getBaseActivity());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

        if (mCurrentTabPressedOnce) {
            Filter filter = mFilters.get(tab.getPosition());
            showFilterDialog(filter);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        AnalyticsHelper.sendEvent(getActivity(),
                AnalyticsHelper.CATEGORY_FILTER,
                AnalyticsHelper.ACTION_CLICK,
                AnalyticsHelper.CATEGORY_FILTER);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mCurrentTabPressedOnce = state == ViewPager.SCROLL_STATE_IDLE;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(PARAM_CURRENT_FILTER, mViewPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    private void setupViewPager(int index) {
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.clearOnPageChangeListeners();
        mViewPager.addOnPageChangeListener(this);
        mFilters = FilterModel.getFilters(getActivity());
        mAdapter = new Adapter(getChildFragmentManager());

        for (int i = 0; i < mFilters.size(); i++) {
            mAdapter.addFragment(EpisodesFragmentPage.newInstance(mFilters.get(i)), mFilters.get(i).getName());
        }
        mViewPager.setAdapter(mAdapter);
        ((PremoActivity) getActivity()).setViewPager(mViewPager);
        mViewPager.setCurrentItem(index);
    }

    /**
     * Shows the Filter Dialog
     */
    private void showFilterDialog(Filter filter) {
        int sortOrder = mAdapter.getCount() - 1;

        if (filter == null) {
            EditFilterActivity.createNewFilter(getActivity(), sortOrder);
        } else {
            EditFilterActivity.editFilter(getActivity(), filter, sortOrder);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check which request we're responding to
        if (resultCode == EditFilterActivity.RESULT_OK) {
            refreshTabs(data.getIntExtra(EditFilterActivity.PARAM_SORT_ORDER, 0));
        }
    }

    public void refreshTabs(int index) {
        setupViewPager(index);
    }

    private static class Adapter extends FragmentStatePagerAdapter {
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