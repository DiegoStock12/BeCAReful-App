package com.becareful.becareful.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import com.becareful.becareful.notif.NotificationUtils;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;



public class DataUpdaterJobService extends JobService {

    private AsyncTask<Void, Void, Void> mFetchDataTask;
    private final String TAG = DataUpdaterJobService.class.getSimpleName();

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        Log.v(TAG, "Empezando nueva sinc");

        mFetchDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Context context = getApplicationContext();
                BecarefulSyncTask.syncStatus(context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(jobParameters, false);
            }
        };

        mFetchDataTask.execute();
        return true;


    }

    @Override
    public boolean onStopJob(JobParameters job) {
        if (mFetchDataTask != null){
            mFetchDataTask.cancel(true);
        }
        return true;
    }


}


