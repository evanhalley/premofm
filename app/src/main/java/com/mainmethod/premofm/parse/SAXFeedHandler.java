package com.mainmethod.premofm.parse;

import android.webkit.URLUtil;

import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.http.HttpHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import timber.log.Timber;

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
    private StringBuilder characters;

    public SAXFeedHandler() {
        characters = new StringBuilder();
        episodeList = new ArrayList<>();
    }

    @Override
    public Feed processXml(String xmlData) throws HttpHelper.XmlDataException {
        InputStream reader = null;

        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(true);
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            reader = new ByteArrayInputStream(xmlData.getBytes("UTF-8"));
            saxParser.parse(reader, this);
        } catch (Exception e) {
            Timber.e(e, "Error in processXml");
            throw new HttpHelper.XmlDataException(HttpHelper.XmlDataException.ERROR_PARSE);
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
            handleEpisodeData(qName, attributes);
        }

        if (isParsingChannel) {
            handleChannelData(qName, attributes);
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
            writeEpisodeData(qName);
        }

        if (isParsingChannel) {
            writeChannelData(qName);
        }
    }

    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {

        if (doReadCharacters) {
            characters.append(new String(ch, start, length));
        }
    }

    private void handleEpisodeData(String qName, Attributes attributes) {

        switch (qName) {
            case "title":
            case "link":
            case "pubDate":
            case "description":
            case "content:encoded":
            case "guid":
            case "itunes:duration":
                doReadCharacters = true;
                characters.setLength(0);
                break;
            case "enclosure":
                loadMediaAttributes(attributes);
                break;
            default:
                break;
        }
    }

    private void writeEpisodeData(String qName) {
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

    private void handleChannelData(String qName, Attributes attributes) {

        switch (qName) {
            case "title":
            case "link":
            case "description":
            case "itunes:author":
                doReadCharacters = true;
                characters.setLength(0);
                break;
            case "itunes:image":
                loadImageAttributes(attributes);
                break;
            default:
                break;
        }
    }

    private void writeChannelData(String qName) {

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

    /**
     * Returns the contents of the character stream
     * @return
     */
    private String getCharacters() {
        String val = characters.toString().trim();
        val = TextHelper.sanitizeString(val);
        doReadCharacters = false;
        characters.setLength(0);
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
            try {
                episode.setRemoteMediaUrl(url.trim());
                episode.setGeneratedId(TextHelper.generateMD5(url));
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                Timber.w("Error in loadMediaAttributes");
                episode.setRemoteMediaUrl(null);
            }
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