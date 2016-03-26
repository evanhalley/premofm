package com.mainmethod.premofm.http;

import android.util.Log;

import com.mainmethod.premofm.helper.ResourceHelper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by evan on 12/1/14.
 */
public class HttpHelper {

    private static final String TAG = HttpHelper.class.getSimpleName();
    private static final int CONNECTION_TIMEOUT = 10_000;
    private static final int BUFFER_SIZE = 8_192;

    /**
     * Returns a HTTPS URL connection object for consumption
     * @param urlStr
     * @return
     * @throws java.io.IOException
     */
    public static HttpURLConnection getConnection(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        Log.d(TAG, "Creating a new connection for " + urlStr);

        if(urlStr != null && urlStr.trim().length() > 0) {
            URL url = new URL(urlStr.trim());

            if(url.getProtocol().toLowerCase().contentEquals("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setUseCaches(false);
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
    private static String readString(InputStream inputStream) throws IOException {
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
        return buffer.toString("UTF-8");
    }

    /**
     * Gets data from a URL with optional headers, returns a response with a status code, headers, and body
     * @param connection
     * @return
     */
    public static Response getData(HttpURLConnection connection) {
        return getData(connection, null);
    }

    /**
     * Gets data from a URL with optional headers, returns a response with a status code, headers, and body
     * @param connection
     * @param header
     * @return
     */
    public static Response getData(HttpURLConnection connection, Map<String, String> header) {
        Response response = null;
        InputStream inputStream = null;

        if (connection == null) {
            return null;
        }
        Log.d(TAG, "Getting data from " + connection.getURL().toString());

        try {
            // configure the connection
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.addRequestProperty("Accept-Encoding", "gzip,deflate");

            // configure the header
            if (header != null) {
                Log.d(TAG, "Adding header information to connection");

                for (String key : header.keySet()) {
                    connection.setRequestProperty(key, header.get(key));
                }
            }

            // check the response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);
            response = new Response();
            response.setResponseCode(responseCode);
            response.setHeaderFields(connection.getHeaderFields());

            // if the response is good, check for a response body
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                inputStream = getInputStream(connection);
                response.setResponseBody(readString(inputStream));
                Log.d(TAG, "Received " + response.getResponseBody().getBytes("UTF-8").length +
                        " bytes");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while getting data");
            Log.e(TAG, e.toString());
        } finally {
            ResourceHelper.closeResources(new Object[]{ inputStream, connection });
        }
        return response;
    }

    /**
     * Send some data to a URL without headers, returns a response with a status code, headers, and body
     * @param connection
     * @param data
     * @return
     */
    public static Response postData(HttpURLConnection connection, String data) {
        return postData(connection, data, null);
    }

    /**
     * Send some data to a URL with optional headers, returns a response with a status code, headers, and body
     * @param connection
     * @param data
     * @param header
     * @return
     */
    public static Response postData(HttpURLConnection connection, String data, Map<String, String> header) {
        Response response = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        if (connection == null || data == null) {
            return null;
        }
        Log.d(TAG, "Posting data to " + connection.getURL().toString());

        try {
            // configure the connection
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.addRequestProperty("Accept-Encoding", "gzip,deflate");

            // configure the header
            if (header != null) {
                Log.d(TAG, "Adding header information to connection");

                for (String key : header.keySet()) {
                    connection.setRequestProperty(key, header.get(key));
                }
            }

            // set up the output stream and send some data
            byte[] dataBytes = data.getBytes("UTF-8");
            Log.d(TAG, "Sending " + dataBytes.length + " bytes");
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(dataBytes);
            outputStream.flush();

            // check the response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);
            response = new Response();
            response.setResponseCode(responseCode);
            response.setHeaderFields(connection.getHeaderFields());

            // if the response is good, check for a response body
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                inputStream = getInputStream(connection);
                response.setResponseBody(readString(inputStream));
                Log.d(TAG, "Received " + response.getResponseBody().getBytes("UTF-8").length + " bytes");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while sending data");
            Log.e(TAG, e.toString());
        } finally {
            ResourceHelper.closeResources(new Object[]{connection, outputStream, inputStream});
        }
        return response;
    }
}
