package com.mainmethod.premofm.object;

import com.mainmethod.premofm.BuildConfig;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evanhalley on 11/6/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class PlaylistTest {

    private static final String PLAYLIST_JSON = "{\"episodeIds\":[\"1232345234\",\"234131fsw\",\"233212334131fsw\"],\"currentIndex\":0}";
    private static final String SERVER_ID_1 = "1232345234";
    private static final String SERVER_ID_2 = "234131fsw";
    private static final String SERVER_ID_3 = "233212334131fsw";

    @Test
    public void testCreateEmptyPlaylistTest() {
        Playlist playlist = new Playlist();
        Assert.assertEquals(false, playlist.next());
        Assert.assertEquals(false, playlist.previous());
        Assert.assertEquals(null, playlist.getCurrentEpisodeServerId());
    }

    @Test
    public void testPlaylistTraversal() {
        Playlist playlist = new Playlist();

        playlist.addToBeginning(SERVER_ID_1);
        Assert.assertEquals(SERVER_ID_1, playlist.getCurrentEpisodeServerId());

        playlist.addToBeginning(SERVER_ID_2);
        Assert.assertEquals(SERVER_ID_2, playlist.getCurrentEpisodeServerId());

        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(SERVER_ID_1, playlist.getCurrentEpisodeServerId());

        Assert.assertEquals(false, playlist.next());

        Assert.assertEquals(true, playlist.previous());
        Assert.assertEquals(SERVER_ID_2, playlist.getCurrentEpisodeServerId());

        Assert.assertEquals(false, playlist.previous());

        playlist.addToEnd(SERVER_ID_3);
        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(SERVER_ID_3, playlist.getCurrentEpisodeServerId());
    }

    @Test
    public void testPlaylistCreationWithList() {
        List<String> serverIds = new ArrayList<>();
        serverIds.add(SERVER_ID_1);
        serverIds.add(SERVER_ID_2);
        serverIds.add(SERVER_ID_3);
        Playlist playlist = new Playlist(serverIds);
        Assert.assertEquals(SERVER_ID_1, playlist.getCurrentEpisodeServerId());
        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(SERVER_ID_2, playlist.getCurrentEpisodeServerId());
        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(SERVER_ID_3, playlist.getCurrentEpisodeServerId());
        Assert.assertEquals(false, playlist.next());
    }

    @Test
    public void testPlaylistCreationWithJson() {
        Playlist playlist = new Playlist(PLAYLIST_JSON);
        Assert.assertEquals(SERVER_ID_1, playlist.getCurrentEpisodeServerId());
        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(SERVER_ID_2, playlist.getCurrentEpisodeServerId());
        Assert.assertEquals(true, playlist.next());
        Assert.assertEquals(SERVER_ID_3, playlist.getCurrentEpisodeServerId());
        Assert.assertEquals(false, playlist.next());
    }
}
