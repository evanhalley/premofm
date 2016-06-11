package com.mainmethod.premofm.parse;

import com.mainmethod.premofm.http.HttpHelper;

/**
 * Created by evanhalley on 6/9/16.
 */
public interface FeedHandler {

    Feed processXml(String xmlData) throws HttpHelper.XmlDataException;

}
