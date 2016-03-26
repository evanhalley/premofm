package com.mainmethod.premofm.api;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.PackageHelper;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.XORHelper;
import com.mainmethod.premofm.http.HttpHelper;
import com.mainmethod.premofm.http.Response;
import com.mainmethod.premofm.object.Category;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.Credential;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Class that manages interaction with the Premo API Server
 * Created by evan on 12/1/14.
 */
public class ApiManager {

    private static final String TAG = ApiManager.class.getSimpleName();

    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String SITE_URL = "siteUrl";
    public static final String FEED_URL = "feedUrl";
    public static final String ARTWORK_URL = "artworkUrl";
    public static final String NETWORK = "network";
    public static final String TAGS = "tags";
    public static final String DURATION = "duration";
    public static final String GUID = "guid";
    public static final String URL = "url";
    public static final String MEDIA_URL = "mediaUrl";
    public static final String SIZE = "size";
    public static final String MIME_TYPE = "mimeType";
    public static final String PROGRESS = "progress";
    public static final String STATUS = "status";
    public static final String AUTHOR = "author";
    public static final String CHANNEL_ID = "channelId";
    public static final String EPISODE_ID = "episodeId";
    public static final String PUBLISHED_AT = "publishedAt";
    public static final String FAVORITE = "favorite";
    public static final String OLD_PASSWORD = "oldPassword";
    public static final String NEW_PASSWORD = "newPassword";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";
    public static final String NICKNAME = "nickname";
    public static final String INFORMATION = "information";
    public static final String REGISTRATION_ID = "registrationId";
    public static final String USER = "user";
    public static final String SIGNED_DATA = "signedData";
    public static final String SIGNATURE = "signature";
    public static final String DEVELOPER_PAYLOAD = "developerPayload";
    public static final String ORDER_ID = "orderId";
    public static final String PRODUCT_ID = "productId";
    public static final String PURCHASES = "purchases";
    public static final String AUTH_TOKEN = "token";
    public static final String USER_ID = "userId";
    public static final String NAME = "name";
    public static final String IS_PUBLIC = "isPublic";
    public static final String TYPE = "type";
    public static final String CHANNEL_IDS = "channelIds";
    public static final String CHANNELS = "channels";
    public static final String EPISODE_IDS = "episodeIds";
    public static final String KEYWORDS = "keywords";
    public static final String UPDATED_AFTER = "updatedAfter";
    public static final String PUBLISHED_BEFORE = "publishedBefore";
    public static final String DEVICE_ID = "deviceId";
    public static final String FEED_URLS = "feedUrls";

    public static final String LIMIT = "limit";
    public static final int LIMIT_VAL = 15;

    private final Context mContext;

    private static ApiManager sInstance;

    public static ApiManager getInstance(Context context) {

        if (sInstance == null) {
            sInstance = new ApiManager(context);
        }
        return sInstance;
    }

    private ApiManager(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Returns an SSL Socket for use with HTTPS connections to the Premo API
     * @return
     * @throws Exception
     */
    private SSLSocketFactory getApiSSLSocket() throws Exception {
        SSLSocketFactory socketFactory = null;
        InputStream in = null;
        int keystoreResId;
        String passkey = XORHelper.decode(BuildConfig.API_PASSKEY, 27);

        // load the appropriate keystore based on the environment
        if (PackageHelper.getEnvironment() == PackageHelper.ENV_PROD) {
            keystoreResId = R.raw.keystore_prod;
        } else {
            keystoreResId = R.raw.keystore_qa;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("BKS");
            in = mContext.getResources().openRawResource(keystoreResId);
            keyStore.load(in, passkey.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keyStore);
            TrustManager[] tms = tmf.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tms, null);
            socketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred in getApiSSLSocket", e);
            throw e;
        } finally {
            ResourceHelper.closeResource(in);
        }
        return socketFactory;
    }

    private boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Returns a http api connection with authorization credential and query parameters
     * @param urlResId API call
     * @param credential Credential to add to the header
     * @return HTTPS connection to the API
     * @throws IOException
     */
    private HttpURLConnection getApiConnection(int urlResId, Map<String, String> queryParams,
                                               Credential credential) throws Exception {

        if (!hasInternetConnection()) {
            Log.d(TAG, "No internet connection available");
            return null;
        }

        StringBuilder url = new StringBuilder();
        url.append(BuildConfig.API_PROTOCOL)
                .append("://")
                .append(BuildConfig.API_HOSTNAME)
                .append(":")
                .append(BuildConfig.API_PORT);

        Uri.Builder builder = Uri.parse(url.toString()).buildUpon();
        builder.appendEncodedPath(mContext.getString(R.string.api_version))
                .appendEncodedPath(mContext.getString(urlResId));

        if (queryParams != null && queryParams.size() > 0) {
            Log.d(TAG, "Adding query parameters");

            for (String key : queryParams.keySet()) {
                builder.appendQueryParameter(key, queryParams.get(key));
            }
        }

        HttpURLConnection connection = HttpHelper.getConnection(builder.build().toString());
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(getApiSSLSocket());
            ((HttpsURLConnection) connection).setHostnameVerifier(new ApiHostnameVerifier());
        }

        Log.d(TAG, "API Url: " + url.toString());

        if (credential != null) {
            connection.setRequestProperty ("Authorization", credential.getAuthorization());
        }
        return connection;
    }

    /**
     * Makes an HTTP GET API call to the Premo API
     * @param urlResId API
     * @param credential Credential to add to the header
     * @return API response
     * @throws ApiException
     */
    private GetApiResponse apiGet(int urlResId, Credential credential) throws ApiException {
        return apiGet(urlResId, null, credential);
    }

    /**
     * Makes an HTTP GET API call to the Premo API
     * @param urlResId API
     * @param queryParams query params to add to the end of the URL
     * @param credential Credential to add to the header
     * @return API response
     * @throws ApiException
     */
    private GetApiResponse apiGet(int urlResId, Map<String, String> queryParams,
                                  Credential credential) throws ApiException {
        HttpURLConnection connection = null;
        GetApiResponse baseResponse = null;

        try {
            connection = getApiConnection(urlResId, queryParams, credential);
            Response response = processResponse(
                    HttpHelper.getData(connection));
            baseResponse = new GetApiResponse(response);
        } catch (Exception e) {
            throw new ApiException("Error connecting to the API: " + e.toString(),
                    ApiException.Error.API_UNAVAILABLE);
        } finally {
            ResourceHelper.closeResource(connection);
        }
        return baseResponse;
    }

    /**
     * Makes an HTTP POST API call to the Premo API
     * @param urlResId API
     * @param credential Credential to add to the header
     * @param data Data to send
     * @return API Response
     * @throws ApiException
     */
    private PostApiResponse apiPost(int urlResId, Credential credential, Object data)
            throws ApiException {
        HttpURLConnection connection = null;
        PostApiResponse baseResponse = null;

        try {
            connection = getApiConnection(urlResId, null, credential);
            Response response = processResponse(
                    HttpHelper.postData(connection, data.toString()));
            baseResponse = new PostApiResponse(response);
        } catch (Exception e) {
            throw new ApiException("Error connecting to the API: " + e.toString(),
                    ApiException.Error.API_UNAVAILABLE);
        } finally {
            ResourceHelper.closeResource(connection);
        }

        if (baseResponse == null) {
            baseResponse = new PostApiResponse();
        }
        return baseResponse;
    }

    /**
     * Processes the response for HTTP protocol errors or null responses
     * @param response Response to process
     * @return Response if response is successfully processed
     * @throws ApiException
     */
    private Response processResponse(Response response) throws ApiException {

        if(response == null) {
            throw new ApiException("Null response object returned, possible connection issues",
                    ApiException.Error.API_UNAVAILABLE);
        }

        if(response.getResponseCode() == HttpsURLConnection.HTTP_FORBIDDEN) {
            throw new ApiException("Authentication error", ApiException.Error.AUTHENTICATION);
        }

        if(response.getResponseCode() == HttpsURLConnection.HTTP_INTERNAL_ERROR) {
            throw new ApiException("Internal Server Error", ApiException.Error.INTERNAL_ERROR);
        }
        return response;
    }

    /**
     * Executes and processes API calls to the server to the episode endpoint
     * @param credential
     * @param params
     * @return
     * @throws ApiException
     */
    private List<Episode> processGetEpisodes(Credential credential, Map<String, String> params,
                                             boolean channelIsSubscribed)
            throws ApiException{
        GetApiResponse response = apiGet(R.string.api_get_episodes, params, credential);
        return getEpisodesFromJsonArray(response.getArray("episodes"), channelIsSubscribed);
    }

    /**
     * Returns a list of episodes from the channels the user is subscribed to
     * @param credential user
     * @return list of episodes
     * @throws ApiException
     */
    public List<Episode> getEpisodes(Credential credential) throws ApiException {
        Log.i(TAG, "Retrieving episode timeline");
        return processGetEpisodes(credential, null, true);
    }

    /**
     * Returns a list of episodes from the channels the user is subscribed to
     * @param credential user
     * @return list of episodes
     * @throws ApiException
     */
    public List<Episode> getNewTimelineEpisodes(Credential credential, long timestamp) throws ApiException {
        Log.i(TAG, "Retrieving episode timeline");
        Map<String, String> queryParams = null;

        if (timestamp != -1) {
            queryParams = new ArrayMap<>(1);
            queryParams.put(UPDATED_AFTER, String.valueOf(timestamp));
        }
        return processGetEpisodes(credential, queryParams, true);
    }

    /**
     * Returns a list of episodes from the channels the user is subscribed to
     * @param credential user
     * @return list of episodes
     * @throws ApiException
     */
    public List<Episode> getChannelEpisodesNewerThan(Credential credential, String channelServerId,
                                                     long newerThanTimestamp) throws ApiException {
        Log.i(TAG, "Retrieving episode timeline");
        Map<String, String> queryParams = new ArrayMap<>(2);
        queryParams.put(CHANNEL_ID, String.valueOf(channelServerId));
        queryParams.put(UPDATED_AFTER, String.valueOf(newerThanTimestamp));
        return processGetEpisodes(credential, queryParams, true);
    }

    /**
     * Returns a list of episodes from the channels the user is subscribed to
     * @param credential user
     * @return list of episodes
     * @throws ApiException
     */
    public List<Episode> getOlderTimelineEpisodes(Credential credential, long timestamp) throws ApiException {
        Log.i(TAG, "Retrieving episode timeline");
        Map<String, String> queryParams = null;

        if (timestamp != -1) {
            queryParams = new ArrayMap<>(1);
            queryParams.put(PUBLISHED_BEFORE, String.valueOf(timestamp));
        }
        return processGetEpisodes(credential, queryParams, true);
    }

    /**
     * Returns a list of episodes from the channels the user is subscribed to
     * @param credential user
     * @return list of episodes
     * @throws ApiException
     */
    public List<Episode> getEpisodes(Credential credential, String channelServerId,
                                     long publishedBefore, boolean isSubcribed) throws ApiException {
        Log.i(TAG, "Retrieving episode timeline");
        Map<String, String> queryParams = null;

        if (channelServerId != null) {
            queryParams = new ArrayMap<>(3);
            queryParams.put(CHANNEL_ID, String.valueOf(channelServerId));
            queryParams.put(LIMIT, String.valueOf(15));
            queryParams.put(PUBLISHED_BEFORE, String.valueOf(publishedBefore));
        }
        return processGetEpisodes(credential, queryParams, isSubcribed);
    }

    /**
     * Returns a list of episodes fromt he channel server IDs
     * @param credential user
     * @return list of episodes
     * @throws ApiException
     */
    public List<Episode> getEpisodes(Credential credential, List<String> channelServerIds) throws ApiException {
        Log.i(TAG, "Retrieving episodes by channel server IDs");
        String[] serverIdArr = new String[channelServerIds.size()];
        channelServerIds.toArray(serverIdArr);
        String channelServerIdStr = TextUtils.join(",", serverIdArr);
        Map<String, String> queryParams = new ArrayMap<>(2);
        queryParams.put(CHANNEL_IDS, String.valueOf(channelServerIdStr));
        queryParams.put(LIMIT, String.valueOf(5));
        return processGetEpisodes(credential, queryParams, true);
    }

    /**
     * Searches for a channel given a search query
     * @param credential
     * @param query search query
     * @return list of channels matching the query
     * @throws ApiException
     */
    public List<Channel> search(Credential credential, String query, int page) throws ApiException {

        if (query == null || query.length() == 0) {
            return null;
        }
        Log.i(TAG, "Search query: " + query);
        Map<String, String> queryParams = new ArrayMap<>(1);
        queryParams.put("q", query);
        queryParams.put("page", String.valueOf(page));
        GetApiResponse response = apiGet(R.string.api_search, queryParams,
                credential);
        return getChannelsFromJsonArray(response.getArray("channels"));
    }

    /**
     * Returns a channel
     * @param credential
     * @param channelServerId
     * @return
     * @throws ApiException
     */
    public Channel getChannel(Credential credential, String channelServerId) throws ApiException {

        if (channelServerId == null || channelServerId.length() == 0) {
            throw new IllegalArgumentException("Channel or channel server ID is null or empty");
        }
        Log.i(TAG, "Retrieving channel: " + channelServerId);
        Channel retrievedChannel = null;
        Map<String, String> queryParams = new ArrayMap<>(1);
        queryParams.put("channelId", channelServerId);
        GetApiResponse response = apiGet(R.string.api_get_channel, queryParams,
                credential);
        JSONObject channelJson = response.getObject("channel");

        if (channelJson != null) {

            try {
                retrievedChannel = ApiHelper.toChannel(channelJson);
            } catch (JSONException e) {
                Log.w(TAG, "Error in getChannel, possible malformed JSON");
                Log.w(TAG, e.toString());
            }
        }
        return retrievedChannel;
    }

    /**
     * Returns a channel
     * @param credential
     * @param iTunesId
     * @return
     * @throws ApiException
     */
    public Channel getChannelByITunesId(Credential credential, String iTunesId) throws ApiException {

        if (iTunesId == null || iTunesId.length() == 0) {
            throw new IllegalArgumentException("iTunes ID is null or empty");
        }
        Log.i(TAG, "Retrieving channel: " + iTunesId);
        Channel retrievedChannel = null;
        Map<String, String> queryParams = new ArrayMap<>(1);
        queryParams.put("iTunesId", iTunesId);
        GetApiResponse response = apiGet(R.string.api_get_channel, queryParams,
                credential);
        JSONObject channelJson = response.getObject("channel");

        if (channelJson != null) {

            try {
                retrievedChannel = ApiHelper.toChannel(channelJson);
            } catch (JSONException e) {
                Log.w(TAG, "Error in getChannel, possible malformed JSON");
                Log.w(TAG, e.toString());
            }
        }
        return retrievedChannel;
    }

    /**
     * Returns a list of categories from the server
     * @param credential
     * @return
     * @throws ApiException
     */
    public List<Category> getCategories(Credential credential) throws ApiException {
        Log.i(TAG, "Retrieving catergories");
        List<Category> categories = new ArrayList<>();
        GetApiResponse response = apiGet(R.string.api_get_categories, credential);
        JSONArray categoriesArray = response.getArray("categories");

        if (categoriesArray != null && categoriesArray.length() > 0) {
            categories = new ArrayList<>(categoriesArray.length());

            for (int i = 0; i < categoriesArray.length(); i++) {
                try {
                    categories.add(new Category(categoriesArray.getJSONObject(i).getInt("id"),
                            categoriesArray.getJSONObject(i).getString("name")));
                } catch (JSONException e) {
                    Log.w(TAG, "Error in getCategories, possible malformed JSON");
                    Log.w(TAG, e.toString());
                }
            }
        }
        return categories;
    }

    /**
     * Searches for a channel given a search query
     * @param credential
     * @param category category
     * @return list of channels matching the query
     * @throws ApiException
     */
    public List<Channel> getChannelsByCategory(Credential credential, String category, int page) throws ApiException {

        if (category == null || category.length() == 0) {
            return null;
        }
        Log.i(TAG, "Category: " + category);
        Map<String, String> queryParams = new ArrayMap<>(1);
        queryParams.put("category", category);
        queryParams.put("page", String.valueOf(page));
        GetApiResponse response = apiGet(R.string.api_get_channels_by_category, queryParams,
                credential);
        return getChannelsFromJsonArray(response.getArray("channels"));
    }

    /**
     * Returns a list of subscriptions the user is subscribed to
     * @param credential
     * @return list of subscriptions
     * @throws ApiException
     */
    public List<Channel> getChannelSubscriptions(Credential credential) throws ApiException {
        Log.i(TAG, "Retrieving channel subscriptions");
        List<Channel> channelList = new ArrayList<>();
        GetApiResponse response = apiGet(R.string.api_get_subscriptions, credential);
        JSONArray channelArray = response.getArray("subscriptions");

        if (channelArray != null && channelArray.length() > 0) {
            channelList = new ArrayList<>(channelArray.length());

            for (int i = 0; i < channelArray.length(); i++) {
                try {
                   channelList.add(ApiHelper.toChannel(channelArray.getJSONObject(i)));
                } catch (JSONException e) {
                    Log.w(TAG, "Error in getChannelSubscriptions, possible malformed JSON");
                    Log.w(TAG, e.toString());
                }
            }
        }
        return channelList;
    }

    /**
     * Returns the list of trending episodes
     * @param credential
     * @return
     * @throws ApiException
     */
    public List<Episode> getTrendingEpisodes(Credential credential) throws ApiException {
        GetApiResponse response = apiGet(R.string.api_get_trending_episodes, credential);
        return getEpisodesFromJsonArray(response.getArray("episodes"), false);
    }

    /**
     * Returns the list of top channels
     * @param credential
     * @return
     * @throws ApiException
     */
    public List<Channel> getTopChannels(Credential credential)  throws ApiException {
        GetApiResponse response = apiGet(R.string.api_get_top_channels, credential);
        return getChannelsFromJsonArray(response.getArray("channels"));
    }

    /**
     * Returns list of trending channels
     * @param credential
     * @return
     * @throws ApiException
     */
    public List<Channel> getTrendingChannels(Credential credential)  throws ApiException {
        GetApiResponse response = apiGet(R.string.api_get_trending_channels, credential);
        return getChannelsFromJsonArray(response.getArray("channels"));
    }

    /**
     * Subscribes the user to the specified channel
     * @param channelServerId
     * @param credential user
     * @return response from the server
     * @throws ApiException
     */
    public PostApiResponse subscribeToChannel(Credential credential, String channelServerId) throws ApiException {
        Log.i(TAG, "Subscribing to channel: " + channelServerId);

        if (channelServerId == null || channelServerId.length() == 0) {
            throw new IllegalArgumentException("Channel or channel server ID is null or empty");
        }
        JSONObject data = new JSONObject();

        try {
            data.put("channelId", channelServerId);
        } catch (JSONException e) {
            Log.e(TAG, "Error subscribing to channel, possible malformed JSON");
            Log.e(TAG, e.toString());
        }
        return apiPost(R.string.api_channel_subscribe, credential, data);
    }

    /**
     * Subscribes the user to the specified channel
     * @param channelServerId
     * @param credential user
     * @return response from the server
     * @throws ApiException
     */
    public PostApiResponse unsubscribeFromChannel(Credential credential, String channelServerId) throws ApiException {
        Log.i(TAG, "Unsubscribing from channel: " + channelServerId);

        if (channelServerId == null || channelServerId.length() == 0) {
            throw new IllegalArgumentException("Channel or channel server ID is null or empty");
        }
        JSONObject data = new JSONObject();

        try {
            data.put(CHANNEL_ID, channelServerId);
        } catch (JSONException e) {
            Log.e(TAG, "Error unsubscribing from channel, possible malformed JSON");
            Log.e(TAG, e.toString());
        }
        return apiPost(R.string.api_channel_unsubscribe, credential, data);
    }

    /**
     * Creates the user using the Premo rAPI
     * @param email User to create
     * @return True if the user was created
     * @throws ApiException
     */
    public PostApiResponse createUser(String email, String password) throws ApiException {
        Log.i(TAG, "Creating user account: " + email);
        JSONObject data = new JSONObject();

        try {
            data.put(EMAIL, email);
            data.put(PASSWORD, password);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating the user, possible malformed JSON");
            Log.e(TAG, e.toString());
        }
        return apiPost(R.string.api_sign_up_user, null, data);
    }

    /**
     * Authenticates the user
     * @param email User to authenticate
     * @return True if authentication is successful
     * @throws ApiException
     */
    public Credential authenticateUser(String email, String password) throws ApiException {
        Log.i(TAG, "Authenticating user: " + email);
        JSONObject data = new JSONObject();
        Credential credential = null;

        try {
            data.put(EMAIL, email);
            data.put(PASSWORD, password);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating the user, possible malformed JSON");
            Log.e(TAG, e.toString());
        }
        PostApiResponse response = apiPost(R.string.api_authenticate_user, null, data);

        if (response != null && response.isSuccessful()) {
            credential = new Credential(
                    response.getString(USER_ID),
                    response.getString(AUTH_TOKEN));
        }
        return credential;
    }

    /**
     * Updates the user's email address
     * @param credential
     * @param email
     * @return
     * @throws ApiException
     */
    public PostApiResponse updateEmail(Credential credential, String email) throws ApiException {
        Log.i(TAG, "Updating user: " + credential.getUserId());
        JSONObject data = new JSONObject();

        try {
            data.put(EMAIL, email);
        } catch (JSONException e) {
            throw new ApiException("Error parsing user json from API: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_update_user_email, credential, data);
    }

    /**
     * Updates the user's nickname
     * @param credential
     * @param nickname
     * @return
     * @throws ApiException
     */
    public PostApiResponse updateNickname(Credential credential, String nickname) throws ApiException {
        Log.i(TAG, "Updating user: " + credential.getUserId());
        JSONObject data = new JSONObject();

        try {
            data.put(NICKNAME, nickname);
        } catch (JSONException e) {
            throw new ApiException("Error parsing user json from API: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_update_user_nickname, credential, data);
    }

    /**
     * Updates the user's password
     * @param credential
     * @param oldPassword
     * @param newPassword
     * @return
     * @throws ApiException
     */
    public PostApiResponse updatePassword(Credential credential, String oldPassword, String newPassword)
            throws ApiException {
        Log.i(TAG, "Syncing episodes for user: " + credential.getUserId());
        JSONObject data = new JSONObject();

        try {
            data.put(OLD_PASSWORD, oldPassword);
            data.put(NEW_PASSWORD, newPassword);
        } catch (JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_update_password, credential, data);
    }

    /**
     * Adds the purchase to the user's account on the server
     * @param credential
     * @param productId
     * @param orderId
     * @param developerPayload
     * @param signature
     * @param signedData
     * @return
     * @throws ApiException
     */
    public PostApiResponse addPurchase(Credential credential, String productId, String orderId,
                                       String developerPayload, String signature, String signedData)
            throws ApiException {

        Log.i(TAG, "Adding purchase for user: " + credential.getUserId());
        JSONObject data = new JSONObject();

        try {
            data.put(PRODUCT_ID, productId);
            data.put(ORDER_ID, orderId);
            data.put(DEVELOPER_PAYLOAD, developerPayload);
            data.put(SIGNATURE, signature);
            data.put(SIGNED_DATA, signedData);
        } catch (JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_add_purchase, credential, data);

    }

    /**
     * Returns the User profile
     * @param credential User who's profile is returned
     * @return User profile
     * @throws ApiException
     */
    public User getUserProfile(Credential credential) throws ApiException {
        Log.i(TAG, "Getting user profile: " + credential.getUserId());
        GetApiResponse response = apiGet(R.string.api_get_profile, null, credential);
        return responseToUser(response.getObject(USER));
    }

    /**
     * Sends the registration ID to the Premo API
     * @param credential User to authenticate
     * @param registrationId Registration ID
     * @return True if the registration ID was sent
     * @throws ApiException
     */
    public PostApiResponse registerDevice(Credential credential, String registrationId,
                                          String deviceId) throws ApiException {
        Log.i(TAG, "Registering device for user: " + credential.getUserId());
        JSONObject data = new JSONObject();

        try {
            data.put(REGISTRATION_ID, registrationId);
            data.put(DEVICE_ID, deviceId);
        } catch(JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_register_device, credential, data);
    }

    /**
     * Sync local episode changes with the user's account on the server
     * @param credential
     * @return
     * @throws ApiException
     */
    public PostApiResponse syncEpisodes(Credential credential, List<Episode> episodeList) throws ApiException {
        Log.i(TAG, "Syncing episodes for user: " + credential.getUserId());
        JSONArray data = new JSONArray();

        try {

            for (int i = 0; i < episodeList.size(); i++) {
                Episode episode = episodeList.get(i);
                JSONObject item = new JSONObject();
                item.put(CHANNEL_ID, episode.getChannelServerId());
                item.put(EPISODE_ID, episode.getServerId());
                item.put(STATUS, episode.getEpisodeStatus());
                item.put(FAVORITE, episode.isFavorite());
                item.put(PROGRESS, episode.getProgress());
                data.put(item);
            }
        } catch(JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_sync_episodes, credential, data);
    }

    /**
     * Returns a list of collections from the server
     * @param credential
     * @return
     * @throws ApiException
     */
    public List<Collection> getCollections(Credential credential) throws ApiException {
        Log.i(TAG, "Retrieving collections");
        List<Collection> collections = new ArrayList<>(10);
        GetApiResponse response = apiGet(R.string.api_get_collections, credential);
        JSONArray collectionsArray = response.getArray("collections");

        if (collectionsArray != null && collectionsArray.length() > 0) {

            for (int i = 0; i < collectionsArray.length(); i++) {

                try {
                    collections.add(ApiHelper.toCollection(collectionsArray.getJSONObject(i)));
                } catch (JSONException e) {
                    Log.w(TAG, "Error in getCategories, possible malformed JSON");
                    Log.w(TAG, e.toString());
                }
            }
        }
        return collections;
    }

    /**
     * Creates or updates a collection on the server, will update if the doUpdate is true
     * @param credential
     * @param collection
     * @param doUpdate
     * @return
     * @throws ApiException
     */
    public PostApiResponse upsertCollection(Credential credential, Collection collection,
                                            boolean doUpdate) throws ApiException {
        Log.i(TAG, "Upserting collection: " + credential.getUserId());
        JSONObject data = new JSONObject();

        try {
            if (doUpdate) {
                data.put(ID, collection.getServerId());
            }
            data.put(NAME, collection.getName());
            data.put(DESCRIPTION, collection.getDescription());
            data.put(TYPE, collection.getType());
            data.put(IS_PUBLIC, false);

            switch (collection.getType()) {
                case Collection.COLLECTION_TYPE_CHANNEL:
                    data.put(CHANNEL_IDS, new JSONArray(collection.getCollectedServerIds()));
                    break;
                case Collection.COLLECTION_TYPE_EPISODE:
                    data.put(EPISODE_IDS, new JSONArray(collection.getCollectedServerIds()));
                    break;
            }
        } catch(JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }

        int urlResId = doUpdate ? R.string.api_update_collections :
                R.string.api_create_collections;
        return apiPost(urlResId, credential, data);
    }

    /**
     * Bulk subscribes to the podcasts referred to by the URL
     * @param credential
     * @param feedUrls
     * @return
     * @throws ApiException
     */
    public PostApiResponse bulkSubscribe(Credential credential, List<String> feedUrls) throws ApiException {
        Log.i(TAG, "bulkSubscribe");
        JSONObject data = new JSONObject();

        try {
            JSONArray urlArray = new JSONArray();

            for (int i = 0; i < feedUrls.size(); i++) {
                urlArray.put(i, feedUrls.get(i));
            }
            data.put(FEED_URLS, urlArray);
        } catch (JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_channel_bulk_subscribe, credential, data);
    }

    /**
     * Deletes a collection on the server
     * @param credential
     * @param serverId
     * @return
     * @throws ApiException
     */
    public PostApiResponse deleteCollection(Credential credential, String serverId)
            throws ApiException{
        Log.i(TAG, "deleteCollection");
        JSONObject data = new JSONObject();

        try {
            data.put(ID, serverId);
        } catch (JSONException e) {
            throw new ApiException("Error parsing user json: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return apiPost(R.string.api_delete_collections, credential, data);
    }

    /**
     * Converts JSON array of episodes to episode list
     * @param jsonArray
     * @return
     */
    private static List<Episode> getEpisodesFromJsonArray(JSONArray jsonArray, boolean channelIsSubscribed) {
        List<Episode> episodeList = null;

        if (jsonArray != null && jsonArray.length() > 0) {
            episodeList = new ArrayList<>(jsonArray.length());

            for (int i = 0; i< jsonArray.length(); i++) {

                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Episode episode = ApiHelper.toEpisode(jsonObject, jsonObject.has("channel"));
                    episode.setChannelIsSubscribed(channelIsSubscribed);
                    episodeList.add(episode);
                } catch (Exception e) {
                    Log.w(TAG, "Error in getChannelsFromJsonArray, possible malformed JSON", e);
                }
            }
        }

        if (episodeList == null) {
            episodeList = new ArrayList<>(0);
        }
        return episodeList;
    }

    /**
     * Converts JSON array of channels to channel list
     * @param jsonArray
     * @return
     */
    public static List<Channel> getChannelsFromJsonArray(JSONArray jsonArray) {
        List<Channel> channelList = null;

        if (jsonArray != null && jsonArray.length() > 0) {
            channelList = new ArrayList<>(jsonArray.length());

            for (int i = 0; i < jsonArray.length(); i++) {

                try {
                    channelList.add(ApiHelper.toChannel(jsonArray.getJSONObject(i)));
                } catch (JSONException e) {
                    Log.w(TAG, "Error in getChannelsFromJsonArray, possible malformed JSON", e);
                }
            }
        }

        if (channelList == null) {
            channelList = new ArrayList<>(0);
        }
        return channelList;
    }

    /**
     * Translates a JSON response from the API into a User object
     * @param object
     * @return
     * @throws ApiException
     */
    public static User responseToUser(JSONObject object) throws ApiException {
        User user = new User();

        try {
            JSONObject info = object.getJSONObject(INFORMATION);
            user.setId(object.getString(ID));
            user.setEmail(info.getString(EMAIL));
            user.setNickname(info.optString(NICKNAME));

            // look for the premium product ID
            JSONArray purchases = object.getJSONArray(PURCHASES);

            /*for (int i = 0; i < purchases.length(); i++) {
                String productId = purchases.getJSONObject(i).getString(PRODUCT_ID);
            }*/

        } catch(JSONException e) {
            throw new ApiException("Error parsing JSON from API: " + e.toString(),
                    ApiException.Error.OTHER);
        }
        return user;
    }

    private static class ApiHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return hostname.contentEquals(BuildConfig.API_HOSTNAME);
        }
    }
}
