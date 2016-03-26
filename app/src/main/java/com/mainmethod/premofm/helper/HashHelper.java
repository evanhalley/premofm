/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import java.security.MessageDigest;

/**
 * Created by evan on 9/2/15.
 */
public class HashHelper {

    public static String generateMd5Hash(String input) throws Exception {

        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        byte[] bytesOfMessage = input.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(bytesOfMessage);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(String.format("%02x", hashBytes[i] & 0xff));
        }
        return sb.toString();
    }
}
