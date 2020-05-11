package com.becareful.becareful;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.becareful.becareful.pojo.UserData;
import com.becareful.becareful.pojo.VehicleProperties;
import com.becareful.becareful.utils.Endpoints;
import com.becareful.becareful.utils.NetworkUtils;
import com.google.android.gms.vision.text.Text;

import java.net.ConnectException;
import java.net.URL;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static boolean sUpdated;
    private static final String TAG = SettingsFragment.class.getSimpleName();


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.pref_becareful);


    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity().getApplicationContext();

        // If the settings are not updated we have to retrieve them from the server

        if (!sUpdated) {
            retrieveVehicleProperties(context);
            //retrieveUserData(context);
        }

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (null != preference) {
            // Update preference summary
            String value = sharedPreferences.getString(preference.getKey(), "");
            setPreferenceSummary(preference, value);
        }

    }


    /**
     * Sets the summary of the preference to the specified value
     *
     * @param preference
     * @param value
     */
    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            // For list preferences, figure out the label of the selected value
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference instanceof EditTextPreference) {
            // Set value to the string
            preference.setSummary(value);
        }
    }


    private void retrieveUserData(Context context){
        UserData userData = null;

        AsyncTask<Void, Void, UserData> userDataAsyncTask = new AsyncTask<Void, Void, UserData>() {


            @Override
            protected UserData doInBackground(Void... voids) {
                UserData data = null;
                URL url = NetworkUtils.getUrl(context, Endpoints.USER_DATA, null);

                try{
                    data = (UserData) NetworkUtils.readJSON(url, UserData.class);
                } catch (ConnectException e){
                    e.printStackTrace();
                }
                return data;


            }
        };

        try{
            userData =
                    userDataAsyncTask.execute().get();
        }catch (Exception e){
            e.printStackTrace();
        }

        // Set the name in the settings screen
        Log.v(TAG, "Retrieved name: "+ userData.getName()+ " " + userData.getSurname());
        // Save the data we found in the Shared Preferences
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putString(getString(R.string.userNameKey), userData.getName()+ " "+ userData.getSurname());
        editor.apply();

    }


    private void retrieveVehicleProperties(Context context) {
        VehicleProperties vehicleProperties = null;

        AsyncTask<Void, Void, VehicleProperties> vehiclePropertiesAsyncTask = new AsyncTask<Void, Void, VehicleProperties>() {
            @Override
            protected VehicleProperties doInBackground(Void... voids) {
                VehicleProperties properties = null;
                URL url = NetworkUtils.getUrl(context, Endpoints.VEHICLE_DATA, null);

                try {
                    // Get the Vehicle properties
                   properties =
                           (VehicleProperties) NetworkUtils.readJSON(url, VehicleProperties.class);

                } catch (ConnectException e) {
                    e.printStackTrace();
                }

                return properties;
            }
        };

        // Get the vehicle Properties in another thread
        try{

            vehicleProperties =
                    vehiclePropertiesAsyncTask.execute().get();

        } catch (Exception e){
            e.printStackTrace();
        }


        // Set the properties
        // Vehicle model
        EditTextPreference vehicleModelPreference =
                (EditTextPreference) findPreference(getString(R.string.pref_vehicle_model_key));
        if (null != vehicleProperties.getModel()) {
            vehicleModelPreference.setText(vehicleProperties.getModel());
            setPreferenceSummary(vehicleModelPreference, vehicleProperties.getModel());
        }

        // Vehicle plate
        EditTextPreference vehiclePlatePreference =
                (EditTextPreference) findPreference(getString(R.string.pref_vehicle_plate_key));
        if (null != vehicleProperties.getPlate()) {
            vehiclePlatePreference.setText(vehicleProperties.getPlate());
            setPreferenceSummary(vehiclePlatePreference, vehicleProperties.getPlate());
        }

        // Vehicle Color
        ListPreference vehicleColorPreference =
                (ListPreference) findPreference(getString(R.string.pref_vehicle_color_key));
        if (null != vehicleProperties.getColor()) {
            vehicleColorPreference.setValue(capitalized(vehicleProperties.getColor()));
            setPreferenceSummary(vehicleColorPreference, capitalized(vehicleProperties.getColor()));
        }

        sUpdated = true;

        Log.v(TAG, "Updated settings from middleware:" +
                "\nModel = " + vehicleProperties.getModel()
                + "\nColor = " + vehicleProperties.getColor()
                + "\nPlate = " + vehicleProperties.getPlate()
                + "\n");

    }

    /**
     * Capitalizes the given string
     *
     * @param s The lowercase string
     * @return The capitalized string
     */
    private String capitalized(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


}
