package com.becareful.becareful.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.becareful.becareful.R;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;


public class NetworkUtils {

    /* URL's for all endpoints */
    private static final String USER_ID_URL = "http://34.76.158.171:8080/user/id?email=";
    private static final String VEHICLE_STATUS_URL = "http://34.76.158.171:8080/vehicle/status?userId=";
    private static final String VEHICLE_ID_URL = "http://34.76.158.171:8080/user/vehicle/id?userId=";
    private static final String VEHICLE_DATA_URL = "http://34.76.158.171:8080/vehicle/properties?userId=";
    private static final String USER_DATA_URL = "http://34.76.158.171:8080/user/data?userId=";
    private static final String TEMP_THRESHOLD_URL = "";

    private static final Gson gson = new Gson();


    /**
     *
     * Returns endpoint URL
     *
     * @param context
     * @param endpoint
     * @param params Only used to retrieve userID, null otherwise
     * @return
     */
    public static URL getUrl(Context context, Endpoints endpoint, String params) {

        String urlString = null;
        String userID = null;

        // If we dont get the params we have to get the userId from the preferences
        if (params == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    context.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
            userID = sharedPreferences.getString(context.getString(R.string.userIdKey), "");
        }


        switch (endpoint) {
            case USERID:
                urlString = USER_ID_URL + params;
                break;
            case VEHICLE_DATA:
                urlString = VEHICLE_DATA_URL + userID;
                break;
            case VEHICLE_STATUS:
                urlString = VEHICLE_STATUS_URL + userID;
                break;
            case VEHICLE_ID:
                urlString = VEHICLE_ID_URL + userID;
                break;
            case USER_DATA:
                urlString = USER_DATA_URL + userID;
                break;
            case TEMP_THRESHOLD:
                urlString = TEMP_THRESHOLD_URL + userID;
                break;
        }

        try {
            URL url = new URL(urlString);
            return url;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * Returns a String representation of the JSON object returned by an
     * external API
     *
     * @param url
     * @param _class Class used to parse json
     * @return
     * @throws ConnectException
     */
    public static Object readJSON(URL url, Class _class) throws ConnectException {

        StringBuffer buffer = null;

        try (Reader reader = new BufferedReader(
                new InputStreamReader(url.openStream()))) {

            if (buffer == null)
                buffer = new StringBuffer();

            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

        } catch (IOException e) {
            throw new ConnectException("Error retrieving data from Server");
        }

        // Here we have everything
        String jsonData = buffer.toString();
        return gson.fromJson(jsonData, _class);

        //return buffer.toString();
    }


}
