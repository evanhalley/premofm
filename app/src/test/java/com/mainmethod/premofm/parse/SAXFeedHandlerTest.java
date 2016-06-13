package com.mainmethod.premofm.parse;

import com.mainmethod.premofm.object.Channel;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by WillowTree, Inc on 6/9/16.
 */

@RunWith(RobolectricTestRunner.class)
public class SAXFeedHandlerTest {

    public String getTestData(String filename) {
        BufferedReader reader = null;
        StringBuilder data = new StringBuilder();

        try {
            reader = new BufferedReader(new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream(filename)));
            String line;

            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (Exception e){
            // nope
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data.toString();
    }

    @Test
    public void parseValidXml() throws Exception {
        String data = getTestData("atp_rss.xml");
        SAXFeedHandler feedHandler = new SAXFeedHandler(new Channel());
        Feed feed = feedHandler.processXml(data);
        Assert.assertNotNull(feed);
        Assert.assertEquals(100, feed.getEpisodeList().size());
        Assert.assertEquals("Accidental Tech Podcast", feed.getChannel().getTitle());
        Assert.assertEquals("Marco Arment, Casey Liss, John Siracusa", feed.getChannel().getAuthor());
        Assert.assertEquals("Three nerds discussing tech, Apple, programming, and loosely related matters.",
                feed.getChannel().getDescription());
    }

}
