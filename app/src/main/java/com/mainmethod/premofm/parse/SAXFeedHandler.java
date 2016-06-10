package com.mainmethod.premofm.parse;

import android.webkit.URLUtil;

import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by evanhalley on 6/9/16.
 */

public class SAXFeedHandler extends DefaultHandler implements FeedHandler {

    private Channel channel;
    private Episode episode;
    private List<Episode> episodeList;
    private boolean isParsingChannel;
    private boolean isParsingEpisode;
    private boolean doReadCharacters;
    private DateParser dateParser;
    private StringBuilder tagContent;

    public SAXFeedHandler() {
        tagContent = new StringBuilder();
        episodeList = new ArrayList<>();
    }

    @Override
    public Feed processXml(String xmlData) {
        InputStream reader = null;

        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(true);
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            reader = new ByteArrayInputStream(xmlData.getBytes("UTF-8"));
            saxParser.parse(reader, this);
        } catch (ParserConfigurationException | SAXException | NumberFormatException e) {
            // TODO
        } catch (Exception e) {
            // TODO
        } finally {
            ResourceHelper.closeResource(reader);
        }
        return new Feed(channel, episodeList);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

        if (qName.contentEquals("channel")) {
            isParsingChannel = true;
            channel = new Channel();
        } else if (qName.contentEquals("item")) {
            isParsingChannel = false;
            isParsingEpisode = true;
            episode = new Episode();
        }

        if (isParsingEpisode) {

            switch (qName) {
                case "title":
                case "link":
                case "pubDate":
                case "description":
                case "content:encoded":
                case "guid":
                case "itunes:duration":
                    doReadCharacters = true;
                    tagContent.setLength(0);
                    break;
                case "enclosure":
                    loadMediaAttributes(attributes);
                    break;
                default:
                    break;
            }
        }

        if (isParsingChannel) {

            switch (qName) {
                case "title":
                case "link":
                case "description":
                case "itunes:author":
                    doReadCharacters = true;
                    tagContent.setLength(0);
                    break;
                case "itunes:image":
                    loadImageAttributes(attributes);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (qName.contentEquals("item")) {
            isParsingEpisode = false;

            if (episode.getRemoteMediaUrl() != null && episode.getRemoteMediaUrl().length() > 0) {
                episodeList.add(episode);
            }
            episode = null;
        }

        if (isParsingEpisode) {

            switch (qName) {
                case "title":
                    episode.setTitle(getCharacters());
                    break;
                case "link":
                    String url = getCharacters();

                    if (URLUtil.isValidUrl(url)) {
                        episode.setUrl(url);
                    }
                    break;
                case "pubDate":
                    episode.setPublishedAt(parseDate(getCharacters()));
                    break;
                case "description":
                    if (episode.getDescription() == null || episode.getDescription().length() == 0) {
                        String description = getCharacters();
                        episode.setDescription(description, true);
                        episode.setDescriptionHtml(description);
                    }
                    break;
                case "content:encoded":
                    String description = getCharacters();
                    episode.setDescription(description, true);
                    episode.setDescriptionHtml(description);
                    break;
                case "itunes:duration":
                    episode.setDuration(parseDuration(getCharacters()));
                    break;
            }
        }

        if (isParsingChannel) {

            switch (qName) {
                case "title":
                    channel.setTitle(getCharacters());
                    break;
                case "link":
                    String url = getCharacters();

                    if (URLUtil.isValidUrl(url)) {
                        channel.setSiteUrl(url);
                    }
                    break;
                case "description":
                    channel.setDescription(getCharacters());
                    break;
                case "itunes:author":
                    channel.setAuthor(getCharacters());
                    break;
            }
        }
    }

    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {

        if (doReadCharacters) {
            tagContent.append(new String(ch, start, length));
        }
    }

    /**
     * Returns the contents of the character stream
     * @return
     */
    private String getCharacters() {
        String val = tagContent.toString().trim();
        val = TextHelper.sanitizeString(val);
        doReadCharacters = false;
        tagContent.setLength(0);
        return val;
    }

    private long parseDuration(String durationStr) {
        return DateParser.parseDuration(durationStr.trim());
    }

    private Date parseDate(String dateStr) {

        if (dateParser == null) {
            dateParser = new DateParser();
        }
        return dateParser.parseDate(dateStr.trim());
    }

    /**
     * Populates the current episode with the artwork details
     * @param attributes
     */
    private void loadImageAttributes(Attributes attributes) {
        String artworkUrl = attributes.getValue("href");

        if (artworkUrl != null && artworkUrl.trim().length() > 0 && URLUtil.isValidUrl(artworkUrl)) {
            channel.setArtworkUrl(artworkUrl.trim());
        }
    }

    /**
     * Populates the current episode with the media details
     * @param attributes
     */
    private void loadMediaAttributes(Attributes attributes) {
        String url = attributes.getValue("url");
        String sizeStr = attributes.getValue("length");
        String mimeType = attributes.getValue("type");

        if (url != null && url.trim().length() > 0 && URLUtil.isValidUrl(url)) {
            episode.setRemoteMediaUrl(url.trim());
            episode.setGeneratedId(TextHelper.generateMD5(url));
        }

        if (mimeType != null && mimeType.trim().length() > 0) {
            episode.setMimeType(mimeType.trim().toLowerCase());
        }

        if (sizeStr != null && sizeStr.trim().length() > 0) {

            try {
                episode.setSize(Integer.parseInt(sizeStr.trim().replace(",", "")));
            } catch (NumberFormatException e) {
                episode.setSize(0);
            }
        }
    }
}
