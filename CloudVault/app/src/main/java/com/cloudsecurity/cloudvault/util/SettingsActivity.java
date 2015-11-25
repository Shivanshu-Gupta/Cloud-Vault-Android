package com.cloudsecurity.cloudvault.util;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.R;

import java.util.List;

/**
 * Created by Noman on 11/9/2015.
 * Activity for handling settings and preferences in the application
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "CloudVault";
    SharedPreferences sharedPref;
    PreferenceScreen myScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "SettingsActivity : onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {

        Log.v(TAG, "SettingsActivity : onBuildHeaders");
        loadHeadersFromResource(R.xml.setting_preferences, target);
    }
//
    @Override
    protected void onResume() {
        super.onResume();
        sharedPref.registerOnSharedPreferenceChangeListener(this);
}

    @Override
    protected void onPause() {
        super.onPause();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

//    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(TAG, "SettingsActivity : onSharedPreferenceChanged : " + key);
        if (key.equals("adding")) {
//            Toast.makeText(this, "add cloud clicked", Toast.LENGTH_LONG).show();
            Intent testIntent = new Intent(getApplicationContext(), AddCloud.class);
            startActivity(testIntent);
        }
        else if(key.equals("endangered_list")) {
            Toast.makeText(this, "Old Value", Toast.LENGTH_SHORT).show();
//            sharedPreferences.
        }
    }


}
