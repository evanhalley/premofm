/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import com.mainmethod.premofm.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by evan on 6/26/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class HashHelperTest {

    @Test
    public void generateMd5HashTest() throws Exception {
        long timestamp = DatetimeHelper.getTimestamp();
        assertEquals(true, timestamp > 0);
        assertTrue(HashHelper.generateMd5Hash("1234567890").contentEquals("e807f1fcf82d132f9bb018ca6738a19f"));
    }
}

