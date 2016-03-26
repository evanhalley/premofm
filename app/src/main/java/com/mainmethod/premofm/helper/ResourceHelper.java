/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.helper;

import android.util.Log;

import java.io.Closeable;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Helper methods for resource management
 * Created by evan on 12/1/14.
 */
public class ResourceHelper {

    private static final String TAG = ResourceHelper.class.getSimpleName();

    /**
     * Closes or disconnections a closable and disconnectable resource
     * @param resource resource to close
     */
    public static void closeResource(Object resource) {

         if (resource == null) {
            return;
        }

        try {
            if (resource instanceof HttpURLConnection) {
                ((HttpURLConnection) resource).disconnect();
            } else if (resource instanceof Closeable) {
                ((Closeable) resource).close();
            } else if (resource instanceof Connection) {
                ((Connection) resource).close();
            } else if (resource instanceof PreparedStatement) {
                ((PreparedStatement) resource).close();
            } else {
                Log.w(TAG, "Unable to close object: " + resource.getClass().getSimpleName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing resources of type: " +
                    resource.getClass().getSimpleName());
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Closes or disconnects closable and disconnectable resources
     * @param resources resource to close
     */
    public static void closeResources(Object[] resources) {

        if (resources != null) {

            for (int i = 0; i < resources.length; i++) {
                closeResource(resources[i]);
            }
        }
    }
}
