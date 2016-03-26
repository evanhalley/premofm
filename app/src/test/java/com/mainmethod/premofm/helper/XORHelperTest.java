/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;

import com.mainmethod.premofm.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * Created by evan on 6/26/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class XORHelperTest {

    @Test
    public void encodeTest() {
        String encoded = XORHelper.encode("test123", 27);
        String decoded = XORHelper.decode(encoded, 27);
        assertEquals(decoded, "test123");
    }
}
