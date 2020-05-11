package com.becareful.becareful;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.becareful.becareful.data.BecarefulDbEntry;
import com.becareful.becareful.notif.NotificationService;
import com.becareful.becareful.notif.NotificationUtils;
import com.becareful.becareful.pojo.UserId;
import com.becareful.becareful.pojo.VehicleId;
import com.becareful.becareful.sync.BecarefulSyncIntentService;
import com.becareful.becareful.sync.BecarefulSyncUtils;
import com.becareful.becareful.utils.Endpoints;
import com.becareful.becareful.utils.NetworkUtils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {


    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String DEGREES_CELSIUS_SYMBOL = "ºC";
    private static final String DEGREES_FAHRENHEIT_SYMBOL = "ºF";

    private static final String DANGEROUS_TEMPERATURE = "DANGER";
    private static final String SAFE_TEMPERATURE = "SAFE";
    private static final String WARNING_TEMPERATURE = "WARNING";

    private static boolean sRefreshing = false;

    //TextViews for all the different text in the screen
    private TextView mLocationName, mCoordinates;
    private TextView mTemperature, mStatus;
    private TextView mLastUpdated;
    private TextView mLastMapUpdate;
    private TextView mPresence;
    private Geocoder mGeocoder;
    private ImageView mThermometerImage;
    private GoogleMap mGoogleMap;


    /**
     * Interface tweaks
     **/
    private SwipeRefreshLayout mPullToRefresh;
    private NestedScrollView mNestedScrollView;
    private View mTouchInterceptor;

    /**
     * UI Refreshing variables
     **/
    private UiRefreshBroadcastReceiver mUiRefreshBroadcastReceiver;
    private IntentFilter mUiRefreshIntentFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        /** Set logo in the action bar **/
        /*getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.becareful_letters);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);*/

        /*Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        final ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(true);
        ab.setDisplayShowTitleEnabled(false);*/

        SharedPreferences sharedPreferences =
                getSharedPreferences(getString(R.string.shared_preferences_file), MODE_PRIVATE);

        /** Get the userID from the middleware **/
        // If the userId is not already initialized
        if (sharedPreferences.getString(getString(R.string.userIdKey), null) == null ) {
            Intent intentFromLogin = getIntent();
            if (intentFromLogin != null) {
                String email = intentFromLogin.getStringExtra("EMAIL");
                if (email != null) {
                    Log.v(TAG, "Getting userID for email " + email);
                    getUserID(email);
                }
            }
        }

        /** Get the vehicleId from the middleware if it is not already initialized**/
        if (sharedPreferences.getString(getString(R.string.vehicleIdKey), null ) == null)
            getVehicleID();


        /** Set on refresh listener of pull to refresh**/
        mPullToRefresh = findViewById(R.id.pull_to_refresh);
        mPullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                sRefreshing = true;
                Intent intentToSyncInmediately = new Intent(getApplicationContext(), BecarefulSyncIntentService.class);
                intentToSyncInmediately.setAction(BecarefulSyncIntentService.ACTION_SYNC_INMEDIATELY);
                startService(intentToSyncInmediately);
            }
        });

        /** Get the map asynchronously **/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Log.v(TAG, "Calling for mapAsync");
        mapFragment.getMapAsync(this);


        /** Get the views **/
        mCoordinates = findViewById(R.id.tv_coordinates);
        mLocationName = findViewById(R.id.tv_city);
        mTemperature = findViewById(R.id.tv_temperature_value);
        mStatus = findViewById(R.id.tv_status_value);
        mLastUpdated = findViewById(R.id.tv_last_updated);
        mPresence = findViewById(R.id.tv_presence_detected_value);
        mLastMapUpdate = findViewById(R.id.tv_geo_timestamp);
        mTouchInterceptor = findViewById(R.id.transparent_view);
        mNestedScrollView = findViewById(R.id.scrollview);

        /** Get the geocoder for translating coordinates to locations **/
        mGeocoder = new Geocoder(this);

        /** Set up the broadcast receiver **/
        mUiRefreshIntentFilter = new IntentFilter();
        mUiRefreshIntentFilter.addAction(BecarefulSyncIntentService.ACTION_REFRESH_UI);
        mUiRefreshBroadcastReceiver = new UiRefreshBroadcastReceiver();

        /** Setup the touch interceptor so when we use the map neither the Scrollview
         * nor the SwipeRefreshLayout intercept the touches intended for the map **/
        setupTouchInterceptor();


        BecarefulSyncUtils.initialize(this);

        /** Setup the background notification service.
         *
         * If the version of Android is O or newer we have to start a foreground service so
         * our notification service doesn't get killed while in the background
         *
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.v(TAG, "Android O or bigger detected, starting service in foreground");
            NotificationUtils.buildServiceNotification(this);
            startForegroundService(new Intent(this, NotificationService.class));
        } else
            startService(new Intent(this, NotificationService.class));

    }

    @Override
    protected void onResume() {
        Log.v(TAG, "OnResume");
        super.onResume();

        /** Register the broadcast receiver for ui changes **/
        registerReceiver(mUiRefreshBroadcastReceiver, mUiRefreshIntentFilter);
        Log.v(TAG, "Registered broadcast receiver");
        BecarefulSyncUtils.loadPreviouslyStoredData(this);

    }

    @Override
    protected void onPause() {
        Log.v(TAG, "OnPause");
        super.onPause();
        unregisterReceiver(mUiRefreshBroadcastReceiver);
        Log.v(TAG, "Unregistered broadcast receiver");
    }


    /**
     * Refreshes the Map with the new coordinates got from the Ui Refresh Intent
     *
     * @param coordinates
     */
    private void refreshMap(LatLng coordinates) {
        mGoogleMap.clear();
        mGoogleMap.addMarker(new MarkerOptions().position(coordinates))
                .setTitle("Current location");
        CameraUpdate myLocation = CameraUpdateFactory.newLatLngZoom(coordinates, 14);
        mGoogleMap.animateCamera(myLocation);

    }

    /**
     * Retrieves the username from the server given the email
     *
     * @param email
     */
    private void getUserID(String email) {

        AsyncTask<String, Void, String> userNameRetrievalTask = new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... params) {
                final String email = params[0];
                String id = null;

                // Get the URL for the ID
                URL url = NetworkUtils.getUrl(getApplicationContext(), Endpoints.USERID, email);

                try {
                    // Parse the ID
                    UserId userId = (UserId) NetworkUtils.readJSON(url, UserId.class);
                    id = userId.getUserId();
                    Log.v(TAG, "Received ID = " + id);

                } catch (ConnectException e) {
                    e.printStackTrace();
                }

                return id;
            }
        };

        try {
            String userId = userNameRetrievalTask
                    .execute(email)
                    .get();

            if (userId != null) {
                // Put the userId in the SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences(
                        getString(R.string.shared_preferences_file), MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.userIdKey), userId);
                editor.commit();
                Log.v(TAG, "UserID saved in SharedPreferences");
            }
            else{
                Log.e(TAG, "Error getting userId");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * Returns the vehicleId for that user
     */
    private void getVehicleID() {

        AsyncTask<Void, Void, String> vehicleIdRetrieverTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {

                String id = null;

                // Get the URL from the settings
                URL url = NetworkUtils.getUrl(getApplicationContext(), Endpoints.VEHICLE_ID, null);

                try {
                    // Parse the ID
                    VehicleId vehicleId = (VehicleId) NetworkUtils.readJSON(url, VehicleId.class);
                    id = vehicleId.getVehicleId();
                    Log.v(TAG, "Received vehicle ID = " + id);

                } catch (ConnectException e) {
                    e.printStackTrace();
                }

                return id;

            }
        };

        try {
            String vehicleId = vehicleIdRetrieverTask
                    .execute()
                    .get();

            if (vehicleId != null) {
                // Put the userId in the SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences(
                        getString(R.string.shared_preferences_file), MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.vehicleIdKey), vehicleId);
                editor.commit();
                Log.v(TAG, "VehicleId saved in SharedPreferences");
            }
            else{
                Log.e(TAG, "Error getting vehicleId");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Checks the new temperature received in the intent and changes the text and color in case
     * the situation changes to danger or safety.
     */
    private void checkTemperature() {

        Resources res = getResources();

        mTemperature = findViewById(R.id.tv_temperature_value);
        mStatus = findViewById(R.id.tv_status_value);
        mThermometerImage = findViewById(R.id.image_thermometer);

        String tempString = mTemperature.getText().toString();
        tempString = tempString.replace(DEGREES_CELSIUS_SYMBOL, "").trim();
        Log.v(TAG, "tempString = " + tempString);
        int temperatureNumber = Integer.parseInt(tempString);

        /** Check whether the temperature exceeds the limits and change color and thermometer
         *
         * DANGER TEMPERATURE: T >= 38
         * WARNING TEMPERATURE: 32 <= T < 38
         * SAFE TEMPERATURE: T < 32
         *
         * **/
        if (temperatureNumber >= 32 && temperatureNumber < 38) {
            mStatus.setText(WARNING_TEMPERATURE);
            mStatus.setTextColor(res.getColor(R.color.colorWarningTemperature));
            mThermometerImage.setImageResource(R.drawable.thermometer_half);
        } else if (temperatureNumber >= 38) {
            mStatus.setText(DANGEROUS_TEMPERATURE);
            mStatus.setTextColor(res.getColor(R.color.colorDangerousTemperature));
            mThermometerImage.setImageResource(R.drawable.thermometer_full);
        } else {
            mStatus.setText(SAFE_TEMPERATURE);
            mStatus.setTextColor(res.getColor(R.color.colorSafeTemperature));
            mThermometerImage.setImageResource(R.drawable.thermometer_empty);

        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;

        Log.v(TAG, "Map is Ready!");
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomGesturesEnabled(true);


        // Set the onClickListener to open the same location as displayed in the map
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Uri mapLocationUri =
                        Uri.parse("geo:"
                                + latLng.latitude + ","
                                + latLng.longitude
                                + "?z=11"
                                + "&q=" + latLng.latitude + "," + latLng.longitude);
                Intent openMapsIntent = new Intent(Intent.ACTION_VIEW);
                openMapsIntent.setData(mapLocationUri);
                if (openMapsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(openMapsIntent);
                }

            }
        });
    }

    /**
     * To prevent erratic functioning of the app whenever the back button is pressed
     * to exit. We just redirect it to go home
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void setupTouchInterceptor() {
        mTouchInterceptor.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, ".onTouch:DOWN");
                        // Disallow ScrollView to intercept touch events.

                        mNestedScrollView.requestDisallowInterceptTouchEvent(true);
                        mPullToRefresh.requestDisallowInterceptTouchEvent(true);

                        return false;

                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, ".onTouch:UP");
                        // Allow ScrollView to intercept touch events.

                        mNestedScrollView.requestDisallowInterceptTouchEvent(false);
                        mPullToRefresh.requestDisallowInterceptTouchEvent(false);

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, ".onTouch:MOVE");
                        mNestedScrollView.requestDisallowInterceptTouchEvent(true);
                        mPullToRefresh.requestDisallowInterceptTouchEvent(true);

                        return false;

                    default:
                        return true;
                }
            }
        });
    }

    /**
     * Broadcast receiver for changes in the UI
     **/
    private class UiRefreshBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v(TAG, "Intent received with new data, updating...");

            String coordinates = intent.getStringExtra(
                    BecarefulDbEntry.COLUMN_COORDINATES);
            String date = intent.getStringExtra(
                    BecarefulDbEntry.COLUMN_DATE);
            double temp = intent.getDoubleExtra(
                    BecarefulDbEntry.COLUMN_TEMPERATURE, 0);
            boolean presence = intent.getBooleanExtra(
                    BecarefulDbEntry.COLUMN_PRESENCE, true);
            String geo_date = intent.getStringExtra(
                    BecarefulDbEntry.COLUMN_GEO_TIMESTAMP);


            mTemperature.setText(String.valueOf((int) temp) + DEGREES_CELSIUS_SYMBOL);
            mCoordinates.setText("(" + coordinates + ")");
            mLastUpdated.setText("last updated: " + date);
            mLastMapUpdate.setText("at " + geo_date);
            mPresence.setText(presence ? "YES" : "NO");

            //Get current location name from maps
            String[] latLng = coordinates.split(",");
            try {
                List<Address> addresses = mGeocoder.getFromLocation(Double.parseDouble(latLng[0].trim()),
                        Double.parseDouble(latLng[1].trim()),
                        1);
                if (null != addresses && !(addresses.isEmpty())) {
                    Address address = addresses.get(0);
                    String country = address.getCountryName();
                    Log.v(TAG, "Country = " + country);
                    String subAdmin = address.getSubAdminArea();
                    String admin = address.getAdminArea();
                    Log.v(TAG, "Admin = " + admin);
                    Log.v(TAG, "SubAdmin = " + address.getSubAdminArea());
                    String locality = address.getLocality();
                    Log.v(TAG, "Locality = " + locality);
                    if (locality != null)
                        mLocationName.setText(locality + ", " + country);
                    else if (subAdmin != null)
                        mLocationName.setText(subAdmin + ", " + country);
                    else mLocationName.setText(admin + ", " + country);
                } else {
                    mLocationName.setText("Location not found");
                }


            } catch (IOException e) {
                Log.v(TAG, "Exception with geocoder");
                e.printStackTrace();
            }


            LatLng coords = new LatLng(Double.parseDouble(latLng[0]),
                    Double.parseDouble(latLng[1]));
            refreshMap(coords);


            // Check if we have to update the color of the
            // status because of the temperature
            checkTemperature();
            if (sRefreshing) {
                sRefreshing = false;
                mPullToRefresh.setRefreshing(false);
            }


        }

    }


}

/*private void setupSharedPreferences(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCarModel.setText(sharedPreferences.getString(getString(R.string.pref_vehicle_model_key), null));
        mCarColor.setText(sharedPreferences.getString(getString(R.string.pref_vehicle_color_key), null));
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v("MAIN", "Se ha cambiado un ajuste con key= "+key);
        if (key.equals(getString(R.string.pref_vehicle_model_key))) {
            Log.v("MAIN", "Se ha cambiado el modelo del coche");
            mCarModel.setText("");
            mCarModel.setText(sharedPreferences.getString(key, null));
        }else if (key.equals(getString(R.string.pref_vehicle_color_key))) {
            Log.v("MAIN", "Se ha cambiado el color del coche");
            mCarColor.setText("");
            mCarColor.setText(sharedPreferences.getString(key, null));
        }

    }*/
