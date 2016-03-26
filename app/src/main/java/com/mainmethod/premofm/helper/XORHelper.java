/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.util.Base64;

/**
 * Created by evan on 6/26/15.
 */
public class XORHelper {

    /**
     * XORs a string, in a by byte fashion using XOR
     * @param input
     * @param key
     * @return Base64 encoded result
     */
    public static String encode(String input, int key) {
        return Base64.encodeToString(xorWithKey(input.getBytes(), key), Base64.DEFAULT);
    }

    /**
     * Decodes a string, in a by byte fashion using XOR
     * @param input Base64 encoded string
     * @param key
     * @return original value
     */
    public static String decode(String input, int key) {
        return new String(xorWithKey(Base64.decode(input, Base64.DEFAULT), key));
    }

    private static byte[] xorWithKey(byte[] a, int key) {
        byte[] out = new byte[a.length];

        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key);
        }
        return out;
    }
}
