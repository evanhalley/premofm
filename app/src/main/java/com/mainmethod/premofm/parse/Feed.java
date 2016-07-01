package com.mainmethod.premofm.parse;

import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;

import java.util.List;

/**
 * Created by evanhalley on 6/9/16.
 */

public class Feed {

    private final Channel channel;
    private final List<Episode> episodeList;

    public Feed(Channel channel, List<Episode> episodeList) {
        this.channel = channel;
        this.episodeList = episodeList;
    }

    public Channel getChannel() {
        return channel;
    }

    public List<Episode> getEpisodeList() {
        return episodeList;
    }
}
