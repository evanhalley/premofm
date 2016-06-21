/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.helper;

import java.io.Closeable;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

import timber.log.Timber;

/**
 * Helper methods for resource management
 * Created by evan on 12/1/14.
 */
public class ResourceHelper {

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
                Timber.w("Unable to close object: %s", resource.getClass().getSimpleName());
            }
        } catch (Exception e) {
            Timber.e(e, "Error closing resources of type: %s", resource.getClass().getSimpleName());
        }
    }

    /**
     * Closes or disconnects closable and disconnectable resources
     * @param resources resource to close
     */
    public static void closeResources(Object... resources) {

        if (resources != null) {

            for (int i = 0; i < resources.length; i++) {
                closeResource(resources[i]);
            }
        }
    }
}
