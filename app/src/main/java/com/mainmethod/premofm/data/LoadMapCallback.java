package com.mainmethod.premofm.data;

import java.util.Map;

/**
 * Created by evanhalley on 11/4/15.
 */
public interface LoadMapCallback<T, U> {

    void onMapLoaded(Map<T, U> maps);

}
