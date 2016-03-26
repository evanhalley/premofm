/*
 * Copyright (C) 2016 Evan Halley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.mainmethod.premofm.ui.adapter;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseIntArray;

/**
 * Provides ability to bind a recycler view to a Android database cursor
 */
public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private static final int INSERTED = 1;
    private static final int REMOVED = 2;
    private static final int CHANGED = 3;
    private static final int ALL = -1;

    private final String mComparisonColumn;
    private final DataSetObserver mDataSetObserver;
    private int mRowIdColumn;
    private Cursor mCursor;
    private boolean mDataValid;

    public CursorRecyclerViewAdapter(Cursor cursor) {
        this(cursor, null);
    }

    public CursorRecyclerViewAdapter(Cursor cursor, String comparisonColumn) {
        mCursor = cursor;
        mComparisonColumn = comparisonColumn;
        mDataValid = cursor != null;
        mRowIdColumn = mDataValid ? mCursor.getColumnIndex("_id") : -1;
        mDataSetObserver = new NotifyingDataSetObserver();

        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {

        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {

        if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mRowIdColumn);
        }
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public abstract void onBindViewHolder(VH viewHolder, Cursor cursor, int position);

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {

        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        onBindViewHolder(viewHolder, mCursor, position);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     */
    public void changeCursor(Cursor cursor) {

        if (mCursor == null) {
            swapCursor(cursor, null);
        } else {
            SparseIntArray changes = null;

            if (cursor != null && cursor != mCursor && !TextUtils.isEmpty(mComparisonColumn)) {
                changes = diffCursors(mCursor, cursor);
            }
            Cursor old = swapCursor(cursor, changes);

            if (old != null) {
                old.close();
            }
        }
    }

    /**
     * Processes two cursors, old/existing cursor and a new cursor, returning a list of indexes who's
     * records were inserted, deleted, or changed
     * @param oldCursor
     * @param newCursor
     * @return
     */
    private SparseIntArray diffCursors(Cursor oldCursor, Cursor newCursor) {

        SparseIntArray changedOrInserted = getChangeOrInsertRecords(oldCursor, newCursor);

        // all records were inserted in new cursor
        if (changedOrInserted.get(ALL) == INSERTED) {
            return changedOrInserted;
        }

        SparseIntArray deleted = getDeletedRecords(oldCursor, newCursor);

        if (deleted.get(ALL) == INSERTED) {
            return deleted;
        }
        SparseIntArray changes = new SparseIntArray(changedOrInserted.size() + deleted.size());

        for (int i = 0; i < changedOrInserted.size(); i++) {
            changes.put(changedOrInserted.keyAt(i), changedOrInserted.valueAt(i));
        }

        for (int i = 0; i < deleted.size(); i++) {
            changes.put(deleted.keyAt(i), deleted.valueAt(i));
        }
        return changes;
    }

    /**
     * Returns a list of indexes of records that were deleted
     * May also return whether or not ALL records were inserted
     * @param oldCursor
     * @param newCursor
     * @return
     */
    private SparseIntArray getDeletedRecords(Cursor oldCursor, Cursor newCursor) {
        SparseIntArray changes = new SparseIntArray();
        int newCursorPosition = newCursor.getPosition();

        if (oldCursor.moveToFirst()) {
            int cursorIndex = 0;

            // loop old cursor
            do {

                if (newCursor.moveToFirst()) {
                    boolean oldRecordFound = false;

                    // loop new cursor
                    do {

                        // we found a record match
                        if (oldCursor.getInt(mRowIdColumn) == newCursor.getInt(mRowIdColumn)) {
                            oldRecordFound = true;
                            break;
                        }
                    } while(newCursor.moveToNext());

                    if (!oldRecordFound) {
                        changes.put(cursorIndex, REMOVED);
                    }
                    cursorIndex++;
                }

            } while (oldCursor.moveToNext());
        }

        // unable to move the old cursor to the first record, all records in new were adde
        else {
            changes.put(ALL, INSERTED);
        }
        newCursor.moveToPosition(newCursorPosition);
        return changes;
    }

    /**
     * Returns an array of indexes who's records were newly inserted or changed
     * Will also return whether or not all the records were inserted or removed
     * @param oldCursor
     * @param newCursor
     * @return
     */
    private SparseIntArray getChangeOrInsertRecords(Cursor oldCursor, Cursor newCursor) {
        SparseIntArray changes = new SparseIntArray();
        int newCursorPosition = newCursor.getPosition();

        if (newCursor.moveToFirst()) {
            int columnIndex = oldCursor.getColumnIndex(mComparisonColumn);
            int cursorIndex = 0;

            // loop
            do {

                if (oldCursor.moveToFirst()) {
                    boolean newRecordFound = false;

                    // loop
                    do {

                        // we found a record match
                        if (oldCursor.getInt(mRowIdColumn) == newCursor.getInt(mRowIdColumn)) {
                            newRecordFound = true;

                            // values are different, this record has changed
                            if (!oldCursor.getString(columnIndex).contentEquals(newCursor.getString(columnIndex))) {
                                changes.put(cursorIndex, CHANGED);
                            }
                            break;
                        }
                    } while (oldCursor.moveToNext());

                    // new record not found in old cursor, it was newly inserted
                    if (!newRecordFound) {
                        changes.put(cursorIndex, INSERTED);
                    }
                    cursorIndex++;
                }

                // unable to move the new cursor, all records in new are inserted
                else {
                    changes.put(ALL, INSERTED);
                    break;
                }
            } while (newCursor.moveToNext());
        }

        // unable to move new cursor to first
        else {
            changes.put(ALL, REMOVED);
        }
        newCursor.moveToPosition(newCursorPosition);
        return changes;
    }

    /**
     *
     * @param newCursor
     * @param changes
     * @return
     */
    private Cursor swapCursor(Cursor newCursor, SparseIntArray changes) {

        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;

        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;

        if (mCursor != null) {

            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
        } else {
            mRowIdColumn = -1;
            mDataValid = false;
        }

        if (changes != null) {
            // process changes
            if (changes.get(ALL) == INSERTED) {
                notifyItemRangeInserted(0, newCursor.getCount());
            } else if (changes.get(ALL) == REMOVED) {
                notifyItemRangeRemoved(0, newCursor.getCount());
            } else {

                for (int i = 0; i < changes.size(); i++) {

                    switch (changes.valueAt(i)) {
                        case CHANGED:
                            notifyItemChanged(changes.keyAt(i));
                            break;
                        case INSERTED:
                            notifyItemInserted(changes.keyAt(i));
                            break;
                        case REMOVED:
                            notifyItemRemoved(changes.keyAt(i));
                            break;
                    }
                }
            }
        } else if (mCursor != null && mComparisonColumn != null) {
            notifyItemRangeInserted(0, mCursor.getCount());
        } else {
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            super.onChanged();
            mDataValid = true;
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
        }
    }
}