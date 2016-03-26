package com.mainmethod.premofm.data;

/**
 * Created by evanhalley on 11/4/15.
 */
public interface LoadCallback<T> {

    void onLoaded(T object);

}
