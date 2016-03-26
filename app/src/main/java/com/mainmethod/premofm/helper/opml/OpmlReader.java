package com.mainmethod.premofm.helper.opml;

import android.util.Log;

import com.mainmethod.premofm.object.Channel;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/** Reads OPML documents. */
public class OpmlReader {
    private static final String TAG = "OpmlReader";

    // ATTRIBUTES
    private boolean isInOpml = false;
    private ArrayList<Channel> elementList;

    /**
     * Reads an Opml document and returns a list of all OPML elements it can
     * find
     *
     * @throws IOException
     * @throws XmlPullParserException
     */
    public ArrayList<Channel> readDocument(Reader reader)
            throws XmlPullParserException, IOException {
        elementList = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(reader);
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:

                    if (xpp.getName().equals(OpmlSymbols.OPML)) {
                        isInOpml = true;
                    } else if (isInOpml && xpp.getName().equals(OpmlSymbols.OUTLINE)) {
                        Channel element = new Channel();

                        final String title = xpp.getAttributeValue(null, OpmlSymbols.TITLE);

                        if (title != null) {
                            Log.i(TAG, "Using title: " + title);
                            element.setTitle(title);
                        } else {
                            Log.i(TAG, "Title not found, using text");
                            element.setTitle(xpp.getAttributeValue(null, OpmlSymbols.TEXT));
                        }
                        element.setFeedUrl(xpp.getAttributeValue(null, OpmlSymbols.XMLURL));
                        element.setSiteUrl(xpp.getAttributeValue(null, OpmlSymbols.HTMLURL));

                        if (element.getFeedUrl() != null) {

                            if (element.getTitle() == null) {
                                Log.i(TAG, "Opml element has no text attribute.");
                                element.setTitle(element.getFeedUrl());
                            }
                            elementList.add(element);
                        }
                    }
                    break;
            }
            eventType = xpp.next();
        }

        return elementList;
    }

}