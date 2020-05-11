package com.becareful.becareful;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String CLIENT_ID = "99080278992-dosfmn1sfs26ebdl20ajp3mmitdqm72o.apps.googleusercontent.com";
    private static final String TAG = LoginActivity.class.getSimpleName();

    private GoogleSignInClient mGoogleSignInClient;

    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);


        // Button listeners
        findViewById(R.id.signInGoogle).setOnClickListener(this);

        // Configure sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(CLIENT_ID)
                .requestEmail()
                .build();

        // Build a google sign in client with the options specified before
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set properties of the button
        SignInButton signInButton = findViewById(R.id.signInGoogle);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is already logged in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            startMainActivity(new Intent(this, MainActivity.class));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.v(TAG, "Email: " + account.getEmail());
            Log.v(TAG, "GID: " + account.getIdToken());

            // Intent to start main activity and with email inside
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("EMAIL", account.getEmail());
            // Send email so we can start the activity with it
            startMainActivity(intent);

        } catch (ApiException e) {
            Log.w(TAG, "SignInResult: Failed with code: " + e.getStatusCode());
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void startMainActivity(Intent intent) {

        // Start the main activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signInGoogle:
                signIn();
                break;
        }
    }
}
