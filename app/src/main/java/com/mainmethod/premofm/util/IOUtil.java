/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mainmethod.premofm.helper.ResourceHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Created by evan on 2/7/15.
 */
public class IOUtil {
    public static final String TAG = IOUtil.class.getSimpleName();
    public static final String PATH_PODCAST_DIRECTORY = "podcasts";
    private static final float FREE_SPACE_MULTIPLIER = 1.5f;

    /**
     * Returns the contents of a file as text
     * @param context
     * @param uri
     * @return
     */
    public static String readTextFromUri(Context context, Uri uri) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder data = new StringBuilder(1_024);

        try {
            inputStream = context.getContentResolver().openInputStream(uri);

            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    data.append(line);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error opening uri: " + uri, e);
        } finally {
            ResourceHelper.closeResources(new Object[] { inputStream, reader });
        }
        return data.toString();
    }

    /**
     * Creates a directory
     * @param context
     * @param path
     * @return
     */
    public static boolean createDirectory(Context context, String path) {
        File dir = new File(context.getExternalFilesDir(null), path);
        return dir.mkdirs();
    }

    /**
     * Returns true if the proposed file can be stored on disk
     * @param context
     * @param proposedFileSize
     * @return
     */
    public static boolean spaceAvailable(Context context, long proposedFileSize) {

        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        // is the proposed file size smaller than the free space that's available
        return proposedFileSize <= (FREE_SPACE_MULTIPLIER *
                context.getExternalFilesDir(null).getFreeSpace());
    }

    /**
     * Deletes an array of files from the local storage
     * @param files
     * @return
     */
    public static boolean deleteFiles(String... files) {

        if (files == null || files.length == 0) {
            return true;
        }
        Log.d(TAG, "Number of files to delete: " + files.length);
        boolean allFilesDeleted = true;

        for (int i = 0; i < files.length; i++) {
            Log.d(TAG, "Deleting file: " + files[i]);
            URI fileUri = URI.create(files[i]);
            File fileToDelete = new File(fileUri);
            boolean deleted = fileToDelete.delete();

            if (!deleted) {
                allFilesDeleted = false;
            }
        }

        return allFilesDeleted;
    }

}
