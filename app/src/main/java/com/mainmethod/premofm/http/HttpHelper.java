package com.mainmethod.premofm.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.object.Channel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import timber.log.Timber;

/**
 * Created by evan on 12/1/14.
 */
public class HttpHelper {

    private static final int CONNECTION_TIMEOUT = 10_000;
    private static final int BUFFER_SIZE = 8_192;

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Returns a HTTPS URL connection object for consumption
     * @param urlStr
     * @return
     * @throws java.io.IOException
     */
    public static HttpURLConnection getConnection(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        Timber.d("Creating a new connection for %s", urlStr);

        if (urlStr != null && urlStr.trim().length() > 0) {
            URL url = new URL(urlStr.trim());

            if(url.getProtocol().toLowerCase().contentEquals("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setUseCaches(true);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
        }
        return connection;
    }

    /**
     * Returns the correct input stream based on the content encoding
     * @param connection
     * @return
     * @throws IOException
     */
    public static InputStream getInputStream(HttpURLConnection connection) throws IOException {
        InputStream inputStream = null;

        if (connection.getContentEncoding() != null) {

            if (connection.getContentEncoding().equalsIgnoreCase("gzip") ||
                    connection.getContentEncoding().equalsIgnoreCase("x-gzip")) {
                inputStream = new GZIPInputStream(connection.getInputStream());
            } else if (connection.getContentEncoding().equalsIgnoreCase("deflate")) {
                inputStream = new DeflaterInputStream(connection.getInputStream());
            }
        }

        if (inputStream == null) {
            inputStream = connection.getInputStream();
        }
        return inputStream;
    }

    /**
     * Retrusn a string from an input streams
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String readData(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = null;

        try {
            buffer = new ByteArrayOutputStream();
            int bytesRead;
            byte[] bytes = new byte[BUFFER_SIZE];

            while ((bytesRead = inputStream.read(bytes, 0, bytes.length)) != -1) {
                buffer.write(bytes, 0, bytesRead);
            }
            buffer.flush();
        } finally {
            ResourceHelper.closeResource(buffer);
        }
        return buffer.toString("UTF-8").trim();
    }

    /**
     * Gets the channels XML data
     * @return
     */
    public static String getXmlData(Channel channel, boolean isRedirect) throws XmlDataException {
        String channelData = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            connection = getConnection(channel.getFeedUrl());

            // only add the http caching functions if we aren't forcing an update and they are present
            if (channel.getLastModified() > -1) {
                connection.addRequestProperty("Last-Modified", String.valueOf(channel.getLastModified()));
            }

            if (channel.getETag() != null && channel.getETag().trim().length() > 0) {
                connection.addRequestProperty("If-None-Match", channel.getETag());
            }
            int responseCode = connection.getResponseCode();
            Timber.d("Response code: %d", responseCode);
            Timber.d("Content-Length: %d", connection.getContentLength());

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                Timber.d("Podcast URL moved permanently: %s", channel.getFeedUrl());
                channel.setFeedUrl(connection.getHeaderField("Location"));

                if (!isRedirect) {
                    return getXmlData(channel, true);
                } else {
                    throw new XmlDataException(XmlDataException.ERROR_PERM_REDIRECT);
                }
            }

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                inputStream = getInputStream(connection);
                channelData = readData(inputStream);

                if (channelData.length() == 0) {
                    throw new XmlDataException(XmlDataException.ERROR_NO_DATA);
                }

                // remove UTF-16 BOM character '\uFEFF
                if (channelData.startsWith("\uFEFF")) {
                    channelData = channelData.replace("\uFEFF", "");
                }

                if (!channelData.startsWith("<?xml") && !channelData.startsWith("<rss")) {
                    throw new XmlDataException(XmlDataException.ERROR_MALFORMED_RSS);
                }
                updateHttpCacheHeaderValues(connection, channel);
                String newMd5 = TextHelper.generateMD5(channelData);
                String oldMd5 = channel.getDataMd5();

                if (oldMd5 == null || oldMd5.length() == 0 || !oldMd5.contentEquals(newMd5)) {
                    channel.setDataMd5(newMd5);
                } else {
                    Timber.d("Data MD5 prevents update for channel %s", channel.getFeedUrl());
                    channelData = null;
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Timber.d("HTTP caching prevents update for channel %s", channel.getFeedUrl());
            } else if (responseCode >= 400 && responseCode <= 500) {
                throw new XmlDataException(XmlDataException.ERROR_HTTP);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            Timber.w(e, "Error in parseChannel");
            throw new XmlDataException(XmlDataException.ERROR_OTHER);
        } finally {
            ResourceHelper.closeResources(inputStream, connection);
        }
        return channelData;
    }

    /**
     * Determines, based on HTTP header values, if a channel needs to be updated
     * @param connection
     */
    private static void updateHttpCacheHeaderValues(HttpURLConnection connection, Channel channel) {
        long lastModified = connection.getLastModified();
        String eTag = connection.getHeaderField("ETag");

        if (lastModified > 0) {
            channel.setLastModified(lastModified);
        }

        if (eTag != null) {
            channel.setETag(eTag);
        }
    }

    public static class XmlDataException extends Exception {
        public static final int ERROR_HTTP = 0;
        public static final int ERROR_MALFORMED_RSS = 1;
        public static final int ERROR_PERM_REDIRECT = 2;
        public static final int ERROR_NO_DATA = 3;
        public static final int ERROR_OTHER = 4;
        public static final int ERROR_PARSE = 5;

        private final int error;

        public XmlDataException(int error) {
            this.error = error;
        }

        public int getError() {
            return error;
        }
    }
}
