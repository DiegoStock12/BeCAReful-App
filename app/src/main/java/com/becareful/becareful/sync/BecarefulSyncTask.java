package com.becareful.becareful.sync;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.becareful.becareful.data.BecarefulDbEntry;
import com.becareful.becareful.data.BecarefulDbHelper;
import com.becareful.becareful.pojo.Status;
import com.becareful.becareful.utils.Endpoints;
import com.becareful.becareful.utils.NetworkUtils;
import com.google.gson.Gson;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;


public class BecarefulSyncTask {

    private static final Calendar calendar = Calendar.getInstance();
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("HH:mm");

    /**
     * Database Helper in order to store and retrieve the data
     **/
    private static BecarefulDbHelper mDbHelper;

    private static final String TAG = BecarefulSyncTask.class.getSimpleName();


    /**
     * Syncs and returns the new current status of the vehicle
     *
     * @param context
     * @return Bundle with the new parameters from the sync
     */
    synchronized public static Bundle syncStatus(Context context) {

        Random rand = new Random();
        Bundle data = null;

        try {

            /** Get the middleware URL to do the request **/
            URL middlewareUrl = NetworkUtils.getUrl(context, Endpoints.VEHICLE_STATUS, null);
            Log.v(TAG, "URL de la consulta= " + middlewareUrl);

            /** Parse the JSON response from the server **/
            Status status = (Status) NetworkUtils.readJSON(middlewareUrl, Status.class);
            Log.v(TAG, "Parsed JSON from the middleware: " + status.toString());

            /** Extract the status object from the JSON **/
            /*Status status = gson.fromJson(middlewareJSONdata, Status.class);
            Log.v(TAG, "Built new status object: " + status.toString());*/


           /* URL dataRequestUrl = NetworkUtils.getUrl(context);
            Log.v(TAG, "URL = " + dataRequestUrl);

            String jsonDataResponse = NetworkUtils.readJSON(dataRequestUrl);
            Log.v(TAG, "JSON = \n" + jsonDataResponse);*/

            /*INSERT DATA IN THE DATABASE: Insert random things and then try to display it*/

            // 1) Create fake data to display
            /*int presence = PRESENCE[rand.nextInt(2)];
            double temperature = TEMPS[rand.nextInt(5)];
            String coordinates = COORDS[rand.nextInt(5)];
            String date = sdf.format(Calendar.getInstance().getTime());*/


            /** Delete all previous entries from the database **/
            mDbHelper = BecarefulDbHelper.getInstance(context);
            mDbHelper.getWritableDatabase().delete(
                    BecarefulDbEntry.TABLE_NAME,
                    null,
                    null
            );

            Log.v(TAG, "Deleted all contents of the table");


            /** Build calendar object in order to convert timestamp to normal time **/
            calendar.setTimeInMillis(status.getTimestamp());
            String measureTime = sdf.format(calendar.getTime());

            ContentValues statusValues = new ContentValues();
            statusValues.put(BecarefulDbEntry.COLUMN_DATE, measureTime);
            statusValues.put(BecarefulDbEntry.COLUMN_TEMPERATURE, status.getTemperature());
            statusValues.put(BecarefulDbEntry.COLUMN_PRESENCE, status.getPresence());
            statusValues.put(BecarefulDbEntry.COLUMN_COORDINATES, status.getCoordinates().getLatitude() + ", "
                    + status.getCoordinates().getLongitude());
            // Maybe the coordinates have different timestamps
            calendar.setTimeInMillis(status.getCoordinates().getTimestamp());
            String geoTime = sdf.format(calendar.getTime());
            statusValues.put(BecarefulDbEntry.COLUMN_GEO_TIMESTAMP, geoTime);

            /*INSERTAMOS NUEVOS DATOS*/
            mDbHelper.getWritableDatabase().insert(
                    BecarefulDbEntry.TABLE_NAME,
                    null,
                    statusValues
            );

            Log.v(TAG, "Nuevos datos insertados en la base de datos");
            Log.v(TAG, "Datos insertados:\n" +
                    "date = " + status.getTimestamp() +
                    " temperature = " + status.getTemperature() +
                    " presence = " + status.getPresence() +
                    " coordinates = " + status.getCoordinates().getLatitude() + ", "
                    + status.getCoordinates().getLongitude() +
                    "geo-timestamp : " + status.getCoordinates().getTimestamp());



            /** Send data to MainActivity **/
            Intent intentUIRefresh = new Intent(BecarefulSyncIntentService.ACTION_REFRESH_UI);
            data = new Bundle();
            data.putString(BecarefulDbEntry.COLUMN_DATE, measureTime);
            data.putString(BecarefulDbEntry.COLUMN_COORDINATES, status.getCoordinates().getLatitude() + ", " + status.getCoordinates().getLongitude());
            data.putDouble(BecarefulDbEntry.COLUMN_TEMPERATURE, status.getTemperature());
            data.putBoolean(BecarefulDbEntry.COLUMN_PRESENCE, status.getPresence() == 1);
            data.putString(BecarefulDbEntry.COLUMN_GEO_TIMESTAMP, geoTime);

            intentUIRefresh.putExtras(data);
            context.sendBroadcast(intentUIRefresh);

            mDbHelper.close();
            Log.v(TAG, "Database closed after service performed iteration");


        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;

    }
}
