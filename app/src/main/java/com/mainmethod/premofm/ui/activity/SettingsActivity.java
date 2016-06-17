/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.ui.fragment.AboutFragment;
import com.mainmethod.premofm.ui.fragment.SettingsFragment;

/**
 * Created by evan on 10/14/14.
 */
public class SettingsActivity extends BaseActivity {

    private static final String PARAM_SHOW_MODE = "showMode";
    public static final int SHOW_SETTINGS = 0;
    public static final int SHOW_ABOUT = 1;

    public static void start(Context context, int showMode) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(PARAM_SHOW_MODE, showMode);
        context.startActivity(intent);
    }

    @Override
    protected int getHomeContentDescriptionResId() {
        return R.string.back;
    }

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        String title = null;
        Fragment fragment = null;

        switch (getIntent().getIntExtra(PARAM_SHOW_MODE, SHOW_SETTINGS)) {
            case SHOW_SETTINGS:
                title = getString(R.string.drawer_action_settings);
                fragment = new SettingsFragment();
                break;
            case SHOW_ABOUT:
                title = getString(R.string.pref_title_about);
                fragment = new AboutFragment();
                break;
            default:
                throw new IllegalStateException("Invalid show mode encountered");
        }

        getFragmentManager().beginTransaction().replace(
                R.id.fragment_container, fragment).commit();
        setHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(title);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settings;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.settings;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}