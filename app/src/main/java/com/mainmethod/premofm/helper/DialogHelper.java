/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.mainmethod.premofm.R;

/**
 * Created by evan on 9/27/15.
 */
public class DialogHelper {

    public static void openWebviewDialog(final Context context, final String title, final String url) {
        Dialog dialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_webview)
                .setNegativeButton(R.string.dialog_close, (dialog1, id) -> {
                    dialog1.dismiss();
                })
                .setPositiveButton(R.string.dialog_open_in_browser, (dialog1, id) -> {
                    IntentHelper.openBrowser(context, url);
                }).create();
        dialog.show();
        ((TextView) dialog.findViewById(R.id.episode_title)).setText(title);
        WebView webView = (WebView) dialog.findViewById(R.id.episode_info);
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                IntentHelper.openBrowser(context, url);
                return true;
            }
        });
    }

    public static void openWebviewDialog(final Context context, final String title, final String url, final String html) {
        Dialog dialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_webview)
                .setNegativeButton(R.string.dialog_close, (dialog1, id) -> {
                    dialog1.dismiss();
                })
                .setPositiveButton(R.string.dialog_open_in_browser, (dialog1, id) -> {
                    IntentHelper.openBrowser(context, url);
                }).create();
        dialog.show();
        ((TextView) dialog.findViewById(R.id.episode_title)).setText(title);
        WebView webView = (WebView) dialog.findViewById(R.id.episode_info);
        webView.loadData(html, "text/html; charset=utf-8", "UTF-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                IntentHelper.openBrowser(context, url);
                return true;
            }
        });
    }

}
