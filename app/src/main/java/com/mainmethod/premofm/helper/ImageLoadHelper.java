/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.LoadCallback;

import java.io.ByteArrayOutputStream;

import timber.log.Timber;

/**
 * Convenience functions for loading images into views or targets
 * Created by evan on 9/11/15.
 */
public class ImageLoadHelper {

    private static final String SHARED_IMAGE = "premofm_shared.jpg";

    static void saveImage(Context context, String imageUrl, LoadCallback<Uri> callback) {

        new AsyncTask<Void, Void, Uri>() {

            @Override
            protected Uri doInBackground(Void... params) {
                ByteArrayOutputStream stream = null;

                try {
                    Bitmap bitmap = Glide.with(context)
                            .load(imageUrl)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(500, 500)
                            .get();
                    stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String uriStr = MediaStore.Images.Media.insertImage(context.getContentResolver(),
                            bitmap, SHARED_IMAGE, null);

                    if (uriStr != null) {
                        return Uri.parse(uriStr);
                    }
                } catch (Exception e) {
                    Timber.e("Error in saveImage");
                } finally {
                    ResourceHelper.closeResource(stream);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Uri uri) {
                callback.onLoaded(uri);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Asynchronously loads an image into a widget ImageView
     * @param context
     * @param imageUrl
     * @param remoteViews
     * @param imageViewResID
     * @param allWidgetIds
     * @param transformation
     */
    public static void loadImageIntoWidget(Context context, String imageUrl,
                                           RemoteViews remoteViews, int imageViewResID,
                                           int[] allWidgetIds, BitmapTransformation transformation) {
        AppWidgetTarget target = new AppWidgetTarget(context, remoteViews,
                imageViewResID, 250, 250, allWidgetIds);
        Glide.with(context)
                .load(imageUrl)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .transform(transformation)
                .into(target);
    }

    /**
     * Asynchronously loads an image into an ImageView
     * @param context
     * @param imageUrl
     * @param imageView
     */
    public static void loadImageIntoView(Context context, String imageUrl, ImageView imageView) {
        loadImageIntoView(context, imageUrl, imageView, -1, -1);
    }

    /**
     * Asynchronously loads an image into an ImageView
     * @param context
     * @param imageUrl
     * @param imageView
     * @param transformation
     */
    public static void loadImageIntoView(Context context, String imageUrl, ImageView imageView,
                                         BitmapTransformation transformation) {
        loadImageIntoView(context, imageUrl, imageView, -1, -1, transformation);
    }

    /**
     * Asynchronously loads an image into an ImageView with a height and width
     * @param context
     * @param imageUrl
     * @param imageView
     * @param width
     * @param height
     */
    public static void loadImageIntoView(Context context, String imageUrl, ImageView imageView,
                                         int width, int height) {
        loadImageIntoView(context, imageUrl, imageView, width, height, null);
    }

    /**
     * Asynchronously loads an image into an ImageView
     * @param context
     * @param imageUrl
     * @param imageView
     * @param width
     * @param height
     * @param transformation
     */
    public static void loadImageIntoView(Context context, String imageUrl, ImageView imageView,
                                         int width, int height, BitmapTransformation transformation) {
        BitmapRequestBuilder<String, Bitmap> builder = Glide.with(context)
                .load(imageUrl)
                .asBitmap()
                .animate(android.R.anim.fade_in)
                .priority(Priority.LOW)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        if (width > -1 && height > -1) {
            builder.override(width, height);
        }

        if (transformation != null) {
            builder.transform(transformation);
        } else {
            builder.placeholder(R.drawable.default_channel_art);
        }

        builder.into(imageView);
    }

    /**
     *
     * @param context
     * @param imageUrl
     * @param listener
     */
    public static void loadImageAsync(final Context context, String imageUrl, final OnImageLoaded listener) {

        Glide.with(context.getApplicationContext())
                .load(imageUrl)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.NORMAL)
                .into(new SimpleTarget<Bitmap>(400, 400) {

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        listener.imageFailed();
                    }

                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        listener.imageLoaded(bitmap);
                    }
                });

    }

    public interface OnImageLoaded {

        void imageLoaded(Bitmap bitmap);
        void imageFailed();
    }
}
