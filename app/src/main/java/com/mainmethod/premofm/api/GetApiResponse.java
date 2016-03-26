/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.api;

import com.mainmethod.premofm.http.Response;

/**
 * Get API response handles encapsulating data from a HTTP GET Premo API Call
 * Created by evan on 7/27/14.
 */
public class GetApiResponse extends BaseApiResponse {

    public GetApiResponse(Response response) throws ApiException {
        super(response);
    }
}
