package com.becareful.becareful.notif;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.becareful.becareful.MainActivity;
import com.becareful.becareful.R;
import com.becareful.becareful.pojo.Coordinates;
import com.becareful.becareful.pojo.Status;
import com.becareful.becareful.sync.BecarefulSyncIntentService;

/**
 * Utility class for creating notifications
 */
public class NotificationUtils {

    /**
     * Identifies our notification
     */
    private static final int NEW_STATUS_NOTIFICATION_ID = 1212;

    /**
     * Identifies notification pending intent
     */
    private static final int NEW_STATUS_PENDING_INTENT_ID = 2121;

    /**
     * Identifies notification channel (necessary for the newest versions of Android)
     */
    private static final String NEW_STATUS_NOTIFICATION_CHANNEL_ID = "becareful_status_notification_channel";

    /**
     * Identifies our notification
     */
    private static final int ALARM_SOUNDING_NOTIFICATION_ID = 1212;

    /**
     * Identifies notification pending intent
     */
    private static final int ALARM_SOUNDING_PENDING_INTENT_ID = 2121;
    /**
     * Identifies notification channel (necessary for the newest versions of Android)
     */
    private static final String ALARM_SOUNDING_NOTIFICATION_CHANNEL_ID = "becareful_alarm_notification_channel";


    /**
     * Identifies the notification channel for our foreground service
     * */
    static final String NOTIFICATION_SERVICE_CHANNEL_ID = "becareful_notification_service_channel";
    static final int SERVICE_FOREGROUND_NOTIFICATION_ID = 1325;

    /**
     * Our ignore action of the notification intent
     */
    private static final int ACTION_IGNORE_PENDING_INTENT_ID = 12;
    private static final int ACTION_CANCEL_ALARM_PENDING_INTENT_ID = 13;

    public static void clearAllNotifications(Context context){
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * Notifies the user that a new set of data has been synced
     *
     * @param context
     * @param data the data from the newest sync
     */
    public static void notifyUserNewStatus(Context context, Status data){

        /** Firstly we extract the data from the bundle **/
        double temperature = data.getTemperature();
        int presence = data.getPresence();
        long timestamp = data.getTimestamp();
        float latitude = data.getCoordinates().getLatitude();
        float longitude = data.getCoordinates().getLongitude();

        /** Notification content for when it's expanded **/
        String notificationContent = "Condiciones del coche:\n" +
                "Coordinates: " + latitude +", "+ longitude +
                "\nTemperature: " + temperature +
                "\nPresence: " + presence;



        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        /** For newer versions of Android we have to set the NotificationChannel **/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel mChannel = new NotificationChannel(
                    NEW_STATUS_NOTIFICATION_CHANNEL_ID,
                    "Emergency Warning Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        /** Then this is common for all versions of Android **/
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NEW_STATUS_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(R.drawable.car_notif_white)
                .setContentTitle("Estado de emergencia en el vehículo")
                .setContentText("Condiciones perjudiciales detectadas")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        notificationContent))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentIntent(context))
                .addAction(cancelAlarmAction(context))
                .addAction(ignoreStatusAction(context))
                .setAutoCancel(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(NEW_STATUS_NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Notify User that the alarm is sounding
     *
     * @param context
     * @param data
     */
    public static void notifyUserAlarmSounding(Context context, Status data){

        /** Firstly we extract the data from the bundle **/
        double temperature = data.getTemperature();
        int presence = data.getPresence();
        long timestamp = data.getTimestamp();
        float latitude = data.getCoordinates().getLatitude();
        float longitude = data.getCoordinates().getLongitude();

        /** Notification content for when it's expanded **/
        String notificationContent = "Situación de emergencia. Se han bajado las ventanas " +
                "y está sonando la alarma";



        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        /** For newer versions of Android we have to set the NotificationChannel **/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel mChannel = new NotificationChannel(
                    ALARM_SOUNDING_NOTIFICATION_CHANNEL_ID,
                    "Emergency Alarm Sounding Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        /** Then this is common for all versions of Android **/
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, ALARM_SOUNDING_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(R.drawable.car_notif_white)
                .setContentTitle("Alarma sonando en el vehículo")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        notificationContent))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentIntent(context))
                .addAction(stopAlarmAction(context))
                .setAutoCancel(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(NEW_STATUS_NOTIFICATION_ID, notificationBuilder.build());
    }


    private static NotificationCompat.Action stopAlarmAction(Context context){
        Intent stopAlarmIntent = new Intent(context, BecarefulSyncIntentService.class);
        stopAlarmIntent.setAction(BecarefulSyncIntentService.ACTION_STOP_ALARM);
        PendingIntent stopAlarmPendingIntent = PendingIntent.getService(
                context,
                ACTION_IGNORE_PENDING_INTENT_ID,
                stopAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action stopAlarmAction = new NotificationCompat.Action(R.drawable.ic_cancel_black,
                "DETENER ALARMA",
                stopAlarmPendingIntent);
        return stopAlarmAction;

    }

    private static NotificationCompat.Action ignoreStatusAction(Context context){
        Intent ignoreStatusIntent = new Intent(context, BecarefulSyncIntentService.class);
        ignoreStatusIntent.setAction(BecarefulSyncIntentService.ACTION_DISMISS_NOTIFICATION);
        PendingIntent ignoreStatusPendingIntent = PendingIntent.getService(
                context,
                ACTION_IGNORE_PENDING_INTENT_ID,
                ignoreStatusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action ignoreStatusAction = new NotificationCompat.Action(R.drawable.ic_cancel_black,
                "OK",
                ignoreStatusPendingIntent);
        return ignoreStatusAction;

    }

    private static NotificationCompat.Action cancelAlarmAction(Context context){
        Intent cancelAlarmIntent = new Intent(context, BecarefulSyncIntentService.class);
        cancelAlarmIntent.setAction(BecarefulSyncIntentService.ACTION_CANCEL_ALARM);
        PendingIntent cancelAlarmPendingIntent = PendingIntent.getService(
                context,
                ACTION_CANCEL_ALARM_PENDING_INTENT_ID,
                cancelAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action cancelAlarmAction = new NotificationCompat.Action(R.drawable.ic_cancel_black,
                "CANCELAR ALARMA",
                cancelAlarmPendingIntent);
        return cancelAlarmAction;

    }

    /**
     * Sets the intent of the notification so when it's pressed it will start our application
     *
     * @param context
     * @return
     */
    private static PendingIntent contentIntent(Context context){
        Intent startActivityIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(
                context,
                NEW_STATUS_PENDING_INTENT_ID,
                startActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Bitmap largeIcon(Context context){
        Resources res = context.getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(res, R.drawable.car_notif_black_);
        return largeIcon;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void buildServiceNotification(Context context){
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel serviceChannel = new NotificationChannel(
                NOTIFICATION_SERVICE_CHANNEL_ID,
                "Notification Foreground Service",
                NotificationManager.IMPORTANCE_NONE);

        serviceChannel.setLightColor(Color.TRANSPARENT);
        serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        notificationManager.createNotificationChannel(serviceChannel);




    }





}
