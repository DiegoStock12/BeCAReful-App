package com.becareful.becareful;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SwitchActivity extends Activity {

    private static final String TAG = SwitchActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            Log.v(TAG, "User is already logged in, starting main activity");
            startLockScreenActivity();
        }
        else {
            Log.v(TAG, "User is not yet logged in, opening LoginActivity");
            startLockScreenActivity();
        }



    }

    private void startMainActivity() {

        Context context = this;
        Class destinationActivityClass = MainActivity.class;
        Intent signInIntent = new Intent(context, destinationActivityClass);
        signInIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(signInIntent);
        finish();
    }

    private void startLockScreenActivity(){
        Context context = this;
        Class destinationActivityClass = LoginActivity.class;
        Intent signInIntent = new Intent(context, destinationActivityClass);
        signInIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(signInIntent);
        finish();
    }

}
