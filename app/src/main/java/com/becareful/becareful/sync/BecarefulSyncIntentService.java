package com.becareful.becareful.sync;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.becareful.becareful.R;
import com.becareful.becareful.notif.NotificationUtils;

public class BecarefulSyncIntentService extends IntentService {


    /**
     * Name of the action used by the intent filter to refresh ui
     */
    public static final String ACTION_SYNC_INMEDIATELY = "sync-inmediately";
    public static final String ACTION_REFRESH_UI = "com.becareful.becareful.ACTION_REFRESH_UI";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss-notification";
    public static final String ACTION_CANCEL_ALARM = "cancel-car-alarm";
    public static final String ACTION_STOP_ALARM = "stop-car-alarm";

    private static final String TAG = BecarefulSyncIntentService.class.getSimpleName();


    public BecarefulSyncIntentService() {
        super("BecarefulSyncIntentService");
    }

    @Override
    protected void onHandleIntent( Intent intent) {
        String action = intent.getAction();

        if (ACTION_SYNC_INMEDIATELY.equals(action))
            BecarefulSyncTask.syncStatus(this);
        else if (ACTION_DISMISS_NOTIFICATION.equals(action)) {
            Log.v(TAG, "Notification Dismissed");
            NotificationUtils.clearAllNotifications(this);
        }else if (ACTION_CANCEL_ALARM.equals(action)){
            SharedPreferences sharedPreferences = getApplicationContext()
                    .getSharedPreferences(getApplicationContext()
                            .getString(R.string.shared_preferences_file), MODE_PRIVATE);
            final String queueName = sharedPreferences.getString(getApplicationContext()
                    .getString(R.string.vehicleIdKey), "");
            new BecarefulSyncUtils.AlarmCancelTask().execute(queueName);
            NotificationUtils.clearAllNotifications(this);
        }else if (ACTION_STOP_ALARM.equals(action)){
            SharedPreferences sharedPreferences = getApplicationContext()
                    .getSharedPreferences(getApplicationContext()
                            .getString(R.string.shared_preferences_file), MODE_PRIVATE);
            final String queueName = sharedPreferences.getString(getApplicationContext()
                    .getString(R.string.vehicleIdKey), "");
            new BecarefulSyncUtils.AlarmStopTask().execute(queueName);
            NotificationUtils.clearAllNotifications(this);

        }
    }
}
