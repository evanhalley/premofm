/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mainmethod.premofm.ui.activity.BaseActivity;

/**
 * Contains functionality common to all fragments in Premo
 * Created by evan on 12/3/14.
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG = BaseFragment.class.getSimpleName();

    protected abstract int getLayoutResourceId();

    protected abstract int getFragmentTitleResourceId();

    public int getMenuResourceId() {
        return -1;
    }

    public boolean hasTabs() {
        return false;
    }

    // return true if the fragment is handling the back key press
    public boolean onBackPressed() {
        return false;
    }

    public void onTabSelected(TabLayout.Tab tab) {
        // override these
    }

    public void onTabReselected(TabLayout.Tab tab) {
        // overrride if using
    }

    protected BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int menuResId = getMenuResourceId();
        setHasOptionsMenu(menuResId > -1);
        return inflater.inflate(getLayoutResourceId(), container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        int titleResourceId = getFragmentTitleResourceId();

        if (titleResourceId > -1) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(titleResourceId);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        int menuResId = getMenuResourceId();

        if (menuResId > -1) {
            Log.d(TAG, "Inflating menu");
            inflater.inflate(menuResId, menu);
        }
    }
}
