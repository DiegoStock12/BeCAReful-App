package com.becareful.becareful.sync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.becareful.becareful.data.BecarefulDbEntry;
import com.becareful.becareful.data.BecarefulDbHelper;
import com.becareful.becareful.notif.NotificationService;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BecarefulSyncUtils {


    private static final int SYNC_INTERVAL_MINUTES = 2;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(SYNC_INTERVAL_MINUTES);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 4;


    private static boolean sInitialized;
    private static final Gson gson = new Gson();

    private static final String BECAREFUL_SYNC_TAG = "becareful-sync";


    private static void scheduleFirebaseJobDispatcherSync(@NonNull final Context context) {

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        Job syncBecarefulJob = dispatcher.newJobBuilder()
                .setService(DataUpdaterJobService.class)
                .setTag(BECAREFUL_SYNC_TAG)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                .setReplaceCurrent(true)
                .build();

        dispatcher.schedule(syncBecarefulJob);

    }

    synchronized public static void initialize(@NonNull final Context context) {

        if (sInitialized) return;

        sInitialized = true;

        scheduleFirebaseJobDispatcherSync(context);

        /*Thread checkForEmpty = new Thread(new Runnable() {
            @Override
            public void run() {

                BecarefulDbHelper dbHelper = BecarefulDbHelper.getInstance(context);
                String[] projection = {BecarefulDbEntry._ID};



                Cursor cursor = dbHelper.getReadableDatabase().query(
                        BecarefulDbEntry.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null
                );

                if (null == cursor || cursor.getCount() == 0) {
                    Log.v(BECAREFUL_SYNC_TAG, "Cursor vacío, empezando sync");
                    startInmediateSync(context);
                }
                else {
                    Log.v(BECAREFUL_SYNC_TAG, "Recargando datos de la base de datos");
                    loadPreviouslyStoredData(context);
                }

                cursor.close();
                dbHelper.close();
                Log.v(BECAREFUL_SYNC_TAG, "Database closed after checking cursor");


            }
        });

        checkForEmpty.start();*/

    }

    /**
     * Instead of waiting for the periodic sync, we instantly sync in case there's no data
     * stored in our database which which to fill the main interface
     *
     * @param context
     */
    public static void startInmediateSync(@NonNull final Context context) {
        Intent intentToSyncInmediately = new Intent(context, BecarefulSyncIntentService.class);
        intentToSyncInmediately.setAction(BecarefulSyncIntentService.ACTION_SYNC_INMEDIATELY);
        context.startService(intentToSyncInmediately);
    }

    /**
     * If there's some recent data in our database, we load it when the app is started.
     * This will be updated next time the sync service runs and retrieves some new info
     * from the server
     *
     * @param context
     */
    public static void loadPreviouslyStoredData(@NonNull final Context context) {

        BecarefulDbHelper dbHelper = BecarefulDbHelper.getInstance(context);
        Log.v(BECAREFUL_SYNC_TAG, "Recuperando datos de la base de datos");

        Cursor cursor = dbHelper.getReadableDatabase().query(
                BecarefulDbEntry.TABLE_NAME,
                BecarefulDbEntry.MAIN_STATUS_PROJECTION,
                null,
                null,
                null,
                null,
                null);

        if (null == cursor || cursor.getCount() == 0) {
            cursor.close();
            dbHelper.close();
            startInmediateSync(context);
        } else {
            cursor.moveToFirst();
            String coordinates = cursor.getString(BecarefulDbEntry.INDEX_COORDINATES);
            String date = cursor.getString(BecarefulDbEntry.INDEX_DATE);
            double temperature = cursor.getDouble(BecarefulDbEntry.INDEX_TEMPERATURE);
            String geo_date = cursor.getString(BecarefulDbEntry.INDEX_GEO_TIMESTAMP);

            // presence is stored as an int and then it's us who 'translate' it to boolean
            int presence = cursor.getInt(BecarefulDbEntry.INDEX_PRESENCE);

            Log.v(BECAREFUL_SYNC_TAG,
                    "Datos recuperados:\n"
                            + "Coordinates = " + coordinates
                            + "\nDate = " + date
                            + "\nTemperature = " + temperature
                            + "\nPresence = " + presence
                            + "\nGeoTimestamp = " + geo_date);

            /* Build intent and send it to the broadcast receiver in the main activity
             * so we can reflect the data in the database on the main screen */
            Intent intentUIRefresh = new Intent(BecarefulSyncIntentService.ACTION_REFRESH_UI);
            Bundle extras = new Bundle();
            extras.putString(BecarefulDbEntry.COLUMN_DATE, date);
            extras.putString(BecarefulDbEntry.COLUMN_COORDINATES, coordinates);
            extras.putDouble(BecarefulDbEntry.COLUMN_TEMPERATURE, temperature);
            extras.putBoolean(BecarefulDbEntry.COLUMN_PRESENCE, presence == 1);
            extras.putString(BecarefulDbEntry.COLUMN_GEO_TIMESTAMP, geo_date);

            cursor.close();
            dbHelper.close();
            Log.v(BECAREFUL_SYNC_TAG, "Database closed after getting old data from database");

            Log.v(BECAREFUL_SYNC_TAG, "Enviando el intent al mainActivity");
            intentUIRefresh.putExtras(extras);
            context.sendBroadcast(intentUIRefresh);
        }


    }

    static class AlarmCancelTask extends AsyncTask<String, Void, Void> {

        private class CancelAlertMessage {
            private String command;

            public CancelAlertMessage(String msg) {
                this.command = msg;
            }

            public String getCommand() {
                return command;
            }
        }

        @Override
        protected Void doInBackground(String... params) {

            String queueName = params[0];
            Connection connection = null;

            /* Initialize connection with rabbitMQ
             */
            ConnectionFactory factory = new ConnectionFactory();
            factory.setPassword(NotificationService.PASSWORD);
            factory.setUsername(NotificationService.USERNAME);
            factory.setPort(NotificationService.RABBIT_NODE_PORT);
            factory.setHost(NotificationService.RABBIT_NODE_IP_ADDRESS);
            factory.setVirtualHost("/");

            /* Try to send the data */
            try {
                connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, true, false, false, null);
                CancelAlertMessage msg = new CancelAlertMessage("cancel");
                channel.basicPublish("", queueName, null, gson.toJson(msg).getBytes());
                Log.v(BECAREFUL_SYNC_TAG, "Sent a message to cancel the alarm");

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try{
                    connection.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }


            return null;
        }
    }

    static class AlarmStopTask extends AsyncTask<String, Void, Void> {

        private class CancelAlertMessage {
            private String command;

            public CancelAlertMessage(String msg) {
                this.command = msg;
            }

            public String getCommand() {
                return command;
            }
        }

        @Override
        protected Void doInBackground(String... params) {

            String queueName = params[0];
            Connection connection = null;

            /* Initialize connection with rabbitMQ
             */
            ConnectionFactory factory = new ConnectionFactory();
            factory.setPassword(NotificationService.PASSWORD);
            factory.setUsername(NotificationService.USERNAME);
            factory.setPort(NotificationService.RABBIT_NODE_PORT);
            factory.setHost(NotificationService.RABBIT_NODE_IP_ADDRESS);
            factory.setVirtualHost("/");

            /* Try to send the data */
            try {
                connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, true, false, false, null);
                CancelAlertMessage msg = new CancelAlertMessage("stop");
                channel.basicPublish("", queueName, null, gson.toJson(msg).getBytes());
                Log.v(BECAREFUL_SYNC_TAG, "Sent a message to cancel the alarm");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try{
                    connection.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }


            return null;
        }
    }

}
