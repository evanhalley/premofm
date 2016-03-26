package com.mainmethod.premofm.media;

/**
 * Created by evanhalley on 12/4/15.
 */
public interface ProgressUpdateListener {

    void onProgressUpdate(long progress, long bufferedProgress, long duration);

}
