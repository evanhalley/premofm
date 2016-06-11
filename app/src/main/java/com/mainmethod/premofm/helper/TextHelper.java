/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper functions for text processing and text validation
 * Created by evan on 12/7/14.
 */
public class TextHelper {

    public static boolean isPasswordValid(String password) {
        return password.length() > 6;
    }

    public static boolean isValidEmail(String target) {

        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static String joinIntegers(Integer[] input) {
        return joinIntegers(input, false);
    }

    public static String joinIntegers(Integer[] input, boolean escape) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < input.length; i++) {

            if (escape) {
                builder.append("'");
            }

            builder.append(input[i]);

            if (escape) {
                builder.append("'");
            }

            if (i < input.length - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public static String joinStrings(String[] input, boolean escape) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < input.length; i++) {

            if (escape) {
                builder.append("'");
            }

            builder.append(input[i]);

            if (escape) {
                builder.append("'");
            }

            if (i < input.length - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public static Integer[] splitToIntArray(String input) {

        if (input == null || input.trim().length() == 0) {
            return new Integer[0];
        }
        String[] stringIds = input.split(",");
        Integer[] ids = new Integer[stringIds.length];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = Integer.parseInt(stringIds[i]);
        }
        return ids;
    }

    /**
     * Extracts text from HTML source code
     * @param htmlSource
     * @return
     */
    public static String getTextFromHtml(String htmlSource) {

        if (htmlSource == null || htmlSource.trim().length() == 0) {
            return null;
        }

        // get the html
        Spanned textSpan = Html.fromHtml(htmlSource);
        textSpan = removeImageSpanObjects(textSpan);
        String htmlData = textSpan.toString().replace("\n", " ").trim();
        return htmlData;
    }

    /**
     * Replaces all image objects in a Spannable with nothing
     *   Essentially removes them
     * @param spanned
     * @return
     */
    private static Spanned removeImageSpanObjects(Spanned spanned) {
        SpannableStringBuilder builder = (SpannableStringBuilder) spanned;
        Object[] spannedObjects = builder.getSpans(0, builder.length(),
                Object.class);

        for (int i = 0; i < spannedObjects.length; i++) {

            if (spannedObjects[i] instanceof ImageSpan) {
                ImageSpan imageSpan = (ImageSpan) spannedObjects[i];
                builder.replace(spanned.getSpanStart(imageSpan),
                        builder.getSpanEnd(imageSpan), "");
            }
        }
        return spanned;
    }

    public static boolean isValidNickname(String nickname) {

        if (TextUtils.isEmpty(nickname)) {
            return false;
        }
        return true;
    }

    /**
     * Removes illegal characters from the input string
     * @param dirty
     * @return
     */
    public static String sanitizeString(String dirty) {

        if (dirty == null) {
            return null;
        }
        return dirty.replaceAll("[\\u2018\\u2019]", "'").replaceAll("[\\u201C\\u201D]", "\"");
    }

    /**
     * Generates an MD5 hash based on the contents of the data
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String generateMD5(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bytes = data.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytes);
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }
}
