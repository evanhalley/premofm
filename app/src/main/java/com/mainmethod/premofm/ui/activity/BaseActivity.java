package com.mainmethod.premofm.ui.activity;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.PremoApp;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.config.ConfigurationManager;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.UpdateHelper;
import com.mainmethod.premofm.helper.XORHelper;
import com.mainmethod.premofm.helper.billing.IabHelper;
import com.mainmethod.premofm.helper.billing.IabResult;
import com.mainmethod.premofm.helper.billing.Purchase;
import com.mainmethod.premofm.object.User;

import org.parceler.Parcels;

/**
 * Created by evan on 12/1/14.
 */
public abstract class BaseActivity extends AppCompatActivity implements UpdateHelper.OnUpdateCompleteListener {

    private static final String TAG = "BaseActivity";

    private static final int REQUEST_CODE = 10001;

    protected abstract void onCreateBase(Bundle savedInstanceState);

    protected abstract int getLayoutResourceId();

    protected int getMenuResourceId() {
        return -1;
    }

    protected int getHomeContentDescriptionResId() {
        return R.string.navigation_drawer;
    }

    protected Toolbar mToolbar;

    private ProgressDialog mProgressDialog;

    private IabHelper mIabHelper;

    private Menu mMenu;

    private boolean userIsOnboarding() {
        return this instanceof OnboardingActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PremoApp) getApplication()).getTracker();
        ConfigurationManager.getInstance(this);
        boolean userIsOnboarding = userIsOnboarding();
        User user = User.load(this);
        boolean userIsSignedIn = user != null;

        /**
         * How this works, we always want to check to see if the user has signed in,
         *  if not show the intro screen
         *
         * If they are on the intro, sign in, or sign up screen continue that workflow
         *
         * If the user has signed in, we want to check to see if the app was updated,
         *  if it was, run any needed migrate
         */

        // user hasn't signed in and isnt' in the process, redirect them to sign in/up
        if (!userIsSignedIn && !userIsOnboarding) {
            // user has not signed in and isn't onboarding, let's onboard them
            Log.i(TAG, "User has not logged in, proceeding to intro screen");
            Intent activityIntent = new Intent(this, OnboardingActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(activityIntent);
            finish();
        }

        // not signed, but the user is onboarding
        else if (!userIsSignedIn && userIsOnboarding) {
            startBaseActivity(savedInstanceState, false);
        }

        // signed in and not onboarding
        else if (userIsSignedIn && !userIsOnboarding) {

            // app was updated, execute the update
            if (UpdateHelper.wasUpdated(this)) {
                Log.d(TAG, "App was updated, executing update");
                UpdateHelper.executeUpdateAsync(this, this);
            }

            // app was not updated
            else {
                startAppWithSignedInUser(savedInstanceState);
            }
        }

        // signed and onboarding, not a legit case
        else {
            throw new IllegalStateException("User is signed in and onboarding, app in bad state");
        }
    }

    @Override
    public void onUpdateComplete() {
        startAppWithSignedInUser(null);
    }

    private void startAppWithSignedInUser(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        boolean isUsersFirstSignIn = args != null &&
                args.containsKey(PremoApp.FLAG_IS_FIRST_SIGN_IN);
        startBaseActivity(savedInstanceState, isUsersFirstSignIn);
    }

    private void startBaseActivity(Bundle savedInstanceState, boolean firstSignIn) {
        int layoutResId = getLayoutResourceId();

        if (layoutResId != -1) {
            setContentView(layoutResId);
            View toolbar = findViewById(R.id.toolbar);

            if (toolbar != null) {
                mToolbar = (Toolbar) toolbar;
                mToolbar.setNavigationContentDescription(getHomeContentDescriptionResId());
                setSupportActionBar(mToolbar);
            }

            if (firstSignIn) {
                Log.d(TAG, "User's first sign in, initiating first application sync");
                AppPrefHelper.getInstance(this).putLong(AppPrefHelper.PROPERTY_FIRST_BOOT,
                        DatetimeHelper.getTimestamp());
            }
        }
        onCreateBase(savedInstanceState);
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        int menuResId = getMenuResourceId();
        boolean hasMenu = menuResId > -1;

        if (hasMenu) {
            getMenuInflater().inflate(menuResId, menu);
            mMenu = menu;
        }
        return hasMenu;
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    protected void setHomeAsUpEnabled(boolean homeAsUpEnabled) {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(homeAsUpEnabled);
        }
    }

    protected Menu getMenu() {
        return mMenu;
    }

    public void startPremoActivity(Class activityClass) {
        startPremoActivity(activityClass, null, null, -1, null);
    }

    public void startPremoActivity(Class activityClass, int flags, Bundle extras) {
        startPremoActivity(activityClass, null, null, flags, extras);
    }
    public void startPremoActivity(Class activityClass, View sharedView, String sharedTag,
                                   int flags, Bundle extras) {
        Intent intent = new Intent(this, activityClass);

        if (flags > -1) {
            intent.setFlags(flags);
        }

        if (extras != null) {
            intent.putExtras(extras);
        }

        if (sharedView != null) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, sharedView, sharedTag);
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    public void startSlideUpPremoActivity(Class activityClass, int flags, Bundle extras) {
        Intent intent = new Intent(this, activityClass);

        if (flags > -1) {
            intent.setFlags(flags);
        }

        if (extras != null) {
            intent.putExtras(extras);
        }

        ActivityOptions options = ActivityOptions.makeCustomAnimation(
                this, R.anim.slide_up, R.anim.do_nothing);
        startActivity(intent, options.toBundle());
    }

    protected void showProgressDialog(String title, String message) {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        mProgressDialog = ProgressDialog.show(this, title, message, true);
    }

    protected void showProgressDialog(int titleResId, int messageResId) {
        showProgressDialog(getString(titleResId), getString(messageResId));
    }

    protected void dismissDialog() {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handled = false;

        if (mIabHelper != null) {
            handled = mIabHelper.handleActivityResult(requestCode, resultCode, data);
        }

        if (!handled) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
