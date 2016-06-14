/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.DialogHelper;
import com.mainmethod.premofm.helper.IntentHelper;

/**
 * Shows app info
 * Created by evan on 10/14/14.
 */
public class AboutFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener {

    public AboutFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);

        Preference pref = findPreference(getString(R.string.pref_key_premo_version));
        pref.setSummary(BuildConfig.VERSION_NAME);
        pref.setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_view_faq)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_rate)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_twitter)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_privacy)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_tos)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_licenses)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_built_by)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_translations)).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.contentEquals(getString(R.string.pref_key_premo_version))) {
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_rate))) {
            IntentHelper.openAppListing(getActivity());
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_twitter))) {
            IntentHelper.openBrowser(getActivity(), getString(R.string.twitter_url));
        }

        else if (key.contentEquals(getString(R.string.pref_key_licenses))) {
            DialogHelper.openWebviewDialog(getActivity(), getString(R.string.pref_title_licenses), getString(R.string.licenses_url));
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_privacy))) {
            DialogHelper.openWebviewDialog(getActivity(), getString(R.string.pref_title_privacy), getString(R.string.privacy_url));
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_tos))) {
            DialogHelper.openWebviewDialog(getActivity(), getString(R.string.pref_title_tos), getString(R.string.tos_url));
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_built_by))) {
            IntentHelper.openBrowser(getActivity(), getString(R.string.raleigh_url));
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_view_faq))) {
            IntentHelper.openBrowser(getActivity(), getString(R.string.help_url));
        }

        else if (key.contentEquals(getString(R.string.pref_key_translations))) {
            DialogHelper.openWebviewDialog(getActivity(), getString(R.string.pref_title_translations),
                    getString(R.string.translations_url));
        }
        return false;
    }
}