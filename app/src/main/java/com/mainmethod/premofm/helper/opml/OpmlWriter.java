package com.mainmethod.premofm.helper.opml;

import android.util.Log;
import android.util.Xml;

import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.object.Channel;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

/**
 * Exports subscribed channels to an OPML file
 */
public class OpmlWriter {

    private static final String TAG = OpmlWriter.class.getSimpleName();
    private static final String ENCODING = "UTF-8";
    private static final String OPML_VERSION = "2.0";
    private static final String OPML_TITLE = "PremoFM Podcasts";

    public void writeDocument(List<Channel> channels, Writer writer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        XmlSerializer xs = Xml.newSerializer();
        xs.setOutput(writer);

        xs.startDocument(ENCODING, false);
        xs.text("\n");
        xs.startTag(null, OpmlSymbols.OPML);
        xs.attribute(null, OpmlSymbols.VERSION, OPML_VERSION);
        xs.text("\n");

        xs.text("  ");
        xs.startTag(null, OpmlSymbols.HEAD);
        xs.text("\n");
        xs.text("    ");
        xs.startTag(null, OpmlSymbols.TITLE);
        xs.text(OPML_TITLE);
        xs.endTag(null, OpmlSymbols.TITLE);
        xs.text("\n");
        xs.text("    ");
        xs.startTag(null, OpmlSymbols.DATE_CREATED);
        xs.text(DatetimeHelper.formatRFC822Date(new Date()));
        xs.endTag(null, OpmlSymbols.DATE_CREATED);
        xs.text("\n");
        xs.text("  ");
        xs.endTag(null, OpmlSymbols.HEAD);
        xs.text("\n");

        xs.text("  ");
        xs.startTag(null, OpmlSymbols.BODY);
        xs.text("\n");

        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            xs.text("    ");
            xs.startTag(null, OpmlSymbols.OUTLINE);
            xs.attribute(null, OpmlSymbols.TEXT, channel.getTitle());
            xs.attribute(null, OpmlSymbols.TITLE, channel.getTitle());
            xs.attribute(null, OpmlSymbols.TYPE, "link");
            xs.attribute(null, OpmlSymbols.XMLURL, channel.getFeedUrl());

            if (channel.getSiteUrl() != null) {
                xs.attribute(null, OpmlSymbols.HTMLURL, channel.getSiteUrl());
            }
            xs.endTag(null, OpmlSymbols.OUTLINE);
            xs.text("\n");
        }
        xs.text("  ");
        xs.endTag(null, OpmlSymbols.BODY);
        xs.text("\n");
        xs.endTag(null, OpmlSymbols.OPML);
        xs.text("\n");
        xs.endDocument();
        Log.d(TAG, "Finished writing document");
    }
}