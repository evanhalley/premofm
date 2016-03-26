package com.mainmethod.premofm.helper;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.view.ToolbarActionItemTarget;

/**
 * Created by evan on 11/10/15.
 */
public class ShowcaseHelper {

    public static void showFilterShowcase(BaseActivity activity) {

        if (!AppPrefHelper.getInstance(activity).hasViewedFilterShowcase()) {
            displayShowcase(activity, R.id.action_filter, R.string.filter_showcase_title,
                    R.string.filter_showcase_message);
            AppPrefHelper.getInstance(activity).setViewedFilterShowcase(true);
        }
    }

    public static void showCollectionShowcase(BaseActivity activity) {

        if (!AppPrefHelper.getInstance(activity).hasViewedCollectionShowcase()) {
            displayShowcase(activity, R.id.action_add_collection, R.string.showcase_collection_title,
                    R.string.showcase_collection_message);
            AppPrefHelper.getInstance(activity).setViewedCollectionShowcase(true);
        }
    }

    public static void showNowPlayingShowcase(BaseActivity activity) {

        if (!AppPrefHelper.getInstance(activity).hasViewedNowPlayingShowcase()) {
            displayShowcase(activity, R.id.action_play_queue, R.string.showcase_playlist_title,
                    R.string.showcase_playlist_message);
            AppPrefHelper.getInstance(activity).setViewedNowPlayingShowcase(true);
        }
    }

    public static void showCategoryShowcase(BaseActivity activity) {

        if (!AppPrefHelper.getInstance(activity).hasViewedCategoryShowcase()) {
            displayShowcase(activity, R.id.action_category, R.string.showcase_category_title,
                    R.string.showcase_category_message);
            AppPrefHelper.getInstance(activity).setViewedCategoryShowcase(true);
        }
    }

    private static void displayShowcase(BaseActivity activity, int menuItemResId, int titleResId, int messageResId) {
        new ShowcaseView.Builder(activity)
                .setTarget(new ToolbarActionItemTarget(activity.getToolbar(), menuItemResId))
                .setStyle(R.style.PremoShowcaseTheme)
                .setContentTitle(titleResId)
                .setContentText(messageResId)
                .hideOnTouchOutside()
                .withMaterialShowcase()
                .build();
    }

}
