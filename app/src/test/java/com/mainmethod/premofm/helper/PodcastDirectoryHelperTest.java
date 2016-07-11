package com.mainmethod.premofm.helper;

import android.net.Uri;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;

/**
 * Created by evanhalley on 12/23/15.
 */
public class PodcastDirectoryHelperTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullUriTest() {
        PodcastDirectoryHelper.getITunesId(null);
    }

    @Test
    public void nullPathSegments() {
        Uri uri = Mockito.mock(Uri.class);
        when(uri.getPathSegments()).thenReturn(null);
        String id = PodcastDirectoryHelper.getITunesId(uri);
        Assert.assertEquals("", id);
    }

    @Test
    public void noITunesIdTest() {
        Uri uri = Mockito.mock(Uri.class);
        when(uri.getPathSegments()).thenReturn(
                Arrays.asList("us", "podcast", "the-vergecast", "430333725"));
        String id = PodcastDirectoryHelper.getITunesId(uri);
        Assert.assertEquals("", id);
    }

    @Test
    public void noPathSegments() {
        Uri uri = Mockito.mock(Uri.class);
        when(uri.getPathSegments()).thenReturn(Collections.singletonList(""));
        String id = PodcastDirectoryHelper.getITunesId(uri);
        Assert.assertEquals("", id);
    }

    @Test
    public void getITunesIdTest() {
        Uri uri = Mockito.mock(Uri.class);
        when(uri.getPathSegments()).thenReturn(
                Arrays.asList("us", "podcast", "the-vergecast", "id430333725"));
        String id = PodcastDirectoryHelper.getITunesId(uri);
        Assert.assertEquals("430333725", id);
    }
}