package com.mainmethod.premofm.ui.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mainmethod.premofm.PremoApp;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.AppPrefHelper;

public class OnboardingActivity extends BaseActivity implements View.OnClickListener,
        ViewPager.OnPageChangeListener {

    private ViewPager mViewPager;
    private OnboardingViewPagerAdapter mPagerAdapter;

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mViewPager = (ViewPager) findViewById(R.id.intro_demo);
        mPagerAdapter = new OnboardingViewPagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        findViewById(R.id.get_started).setOnClickListener(this);
        ((TextView) findViewById(R.id.privacy_and_tos)).setMovementMethod(new LinkMovementMethod());
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_onboarding;
    }

    @Override
    protected int getMenuResourceId() {
        return -1;
    }

    @Override
    public void onClick(final View v) {

        switch (v.getId()) {
            case R.id.get_started:
                AnalyticsHelper.sendEvent(OnboardingActivity.this, AnalyticsHelper.CATEGORY_TRY_IT,
                        AnalyticsHelper.ACTION_CLICK, "");
                showProgressDialog(R.string.progress_authenticate_title,
                        R.string.progress_authenticate_message);
                AppPrefHelper.getInstance(this).setUserHasOnboarded();
                Bundle bundle = new Bundle();
                bundle.putBoolean(PremoApp.FLAG_IS_FIRST_SIGN_IN, true);
                startPremoActivity(PremoActivity.class, Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK, bundle);
                finish();
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        AnalyticsHelper.sendEvent(this,
                AnalyticsHelper.CATEGORY_ONBOARDING,
                AnalyticsHelper.ACTION_CLICK,
                String.valueOf(position));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private static class OnboardingViewPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = LayoutInflater.from(container.getContext()).inflate(
                    R.layout.item_intro_card, container, false);
            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }
}
