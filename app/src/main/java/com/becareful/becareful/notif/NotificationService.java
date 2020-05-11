package com.becareful.becareful.notif;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.becareful.becareful.R;
import com.becareful.becareful.pojo.Status;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Background running service always waiting for alerts from the server.
 * This Service is responsible for handling server sent callbacks from the RabbitMQ
 * module.
 */
public class NotificationService extends Service {

    private static final String TAG = NotificationService.class.getSimpleName();

    /**
     * Network state variables
     **/
    private static String networkState = null;
    private static final String DATA = "datos";
    private static final String WIFI = "wifi";



    /**
     * Objects to detect network changes and therefore restart connection
     **/
    private ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {

                /**
                 * When the connection changes we restart the connection
                 * @param network
                 */
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.i(TAG, "Connected to "
                            + (connectivityManager.isActiveNetworkMetered() ? "DATOS" : "WIFI"));
                    networkState = (connectivityManager.isActiveNetworkMetered() ?
                            DATA : WIFI);


                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    Log.i(TAG, "Losing active connection");
                    //Log.v(TAG, "Connection closed, trying to start new connection");

                }
            };

    private static Gson gson;

    /**
     * Parameters necessary to connect to our RabbitMQ node
     **/
    public static final String USERNAME = "becareful";
    public static final String PASSWORD = "becarefulsecret";
    public static final String RABBIT_NODE_IP_ADDRESS = "34.76.45.90";
    public static final int RABBIT_NODE_PORT = 5672;
    private static String QUEUE_NAME = null;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        Log.v(TAG, "Notification Service created!");

        /** If we're in Android O we need to start foreground so our service doesn't get killed **/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.v(TAG, "Android O detected, starting foreground service");
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), NotificationUtils.NOTIFICATION_SERVICE_CHANNEL_ID)
                    .setPriority(NotificationCompat.PRIORITY_MIN);

            startForeground(NotificationUtils.SERVICE_FOREGROUND_NOTIFICATION_ID, notificationBuilder.build());
            Log.v(TAG, "Foreground started");
        }

        /** Register the networkCallback to check for connectivity changes **/
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(networkCallback);

        /** Create the new Thread for listening to Rabbit callbacks from the server and
         * start listening **/
        new Thread(new RabbitMQCallbackListener()).start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Notification Service started!");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.v(TAG, "OnTaskRemoved");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent restartService = new Intent(getApplicationContext(), this.getClass());
            restartService.setPackage(getPackageName());
            PendingIntent restartServicePI = PendingIntent.getService(
                    getApplicationContext(), FLAG_UPDATE_CURRENT, restartService,
                    PendingIntent.FLAG_ONE_SHOT);

            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000, restartServicePI);
            Log.v(TAG, "Pending intent set");
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service Stopped");
    }


    /**
     * Runnable thread that will stay waiting for callbacks from the RabbitMQ server forever
     **/
    private class RabbitMQCallbackListener implements Runnable {


        /**
         * RabbitMQ connection objects
         **/
        private ConnectionFactory factory;


        @Override
        public void run() {

            /** Initialize Gson to deserialize the JSON coming from the server **/
            gson = new Gson();

            // Get the queue name
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(
              getApplicationContext().getString(R.string.shared_preferences_file), MODE_PRIVATE);
            QUEUE_NAME = sharedPreferences
                    .getString(getApplicationContext().getString(R.string.userIdKey), "");
            Log.v(TAG, "Queue Name = " + QUEUE_NAME);

            /** Set the basic parameters of the connection**/
            factory = new ConnectionFactory();
            factory.setVirtualHost("/");
            factory.setHost(RABBIT_NODE_IP_ADDRESS);
            factory.setPort(RABBIT_NODE_PORT);
            factory.setUsername(USERNAME);
            factory.setPassword(PASSWORD);
            /** Open connection and channel variables **/

            try {


                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                Log.v(TAG, " [*] Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.v(TAG, " [x] Received '" + message + "'");
                    Status status = gson.fromJson(message, Status.class);
                    if (!status.isAlarmSounding())
                        NotificationUtils.notifyUserNewStatus(getApplicationContext(), status);
                    else {
                        NotificationUtils.clearAllNotifications(getApplicationContext());
                        NotificationUtils.notifyUserAlarmSounding(getApplicationContext(), status);
                    }

                };
                channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
                });


            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }


        }


    }


}

