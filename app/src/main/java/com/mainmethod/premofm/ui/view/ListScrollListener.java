/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.view;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Allows recycler views in PremoFM to implement continuous scrolling and loading
 * Created by evan on 9/10/15.
 */
public class ListScrollListener extends RecyclerView.OnScrollListener {

    private final int VISIBLE_THRESHOLD = 5;

    private int mPreviousTotal = 0;
    private boolean mLoading = true;
    private int mVisibleItemCount;
    private int mTotalItemCount;
    private int mFirstVisibleItem;
    private final OnLoadMoreListener mListener;

    public ListScrollListener(OnLoadMoreListener listener) {
        mListener = listener;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        mVisibleItemCount = recyclerView.getChildCount();
        mTotalItemCount = recyclerView.getLayoutManager().getItemCount();
        mFirstVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();

        if (mLoading) {

            if (mTotalItemCount > mPreviousTotal) {
                mLoading = false;
                mPreviousTotal = mTotalItemCount;
            }
        }

        if (!mLoading && (mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem + VISIBLE_THRESHOLD)) {
            mLoading = true;
            mListener.load();
        }
    }

    public interface OnLoadMoreListener {
        void load();
    }
}