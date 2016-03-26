package com.mainmethod.premofm.data;

import java.util.List;

/**
 * Created by evanhalley on 11/4/15.
 */
public interface LoadListCallback<T> {

    void onListLoaded(List<T> list);

}
