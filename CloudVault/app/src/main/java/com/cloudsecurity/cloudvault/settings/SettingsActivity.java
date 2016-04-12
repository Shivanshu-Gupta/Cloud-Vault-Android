package com.cloudsecurity.cloudvault.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.cloudsecurity.cloudvault.R;

/**
 * Created by Noman on 11/9/2015.
 * Activity for handling settings and preferences in the application
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "CloudVault";

//    private AppCompatDelegate mDelegate;

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "SettingsActivity : onCreate");
//        getDelegate().installViewFactory();
//        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

//        // Setup Toolbar with Up button Leading to Main Activity.
//        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(myToolbar);
//        // Get a support ActionBar corresponding to this toolbar
//        ActionBar ab = getSupportActionBar();
//        // Enable the Up button
//        ab.setDisplayHomeAsUpEnabled(true);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

//    @Override
//    public void onBuildHeaders(List<Header> target) {
//
//        Log.v(TAG, "SettingsActivity : onBuildHeaders");
//        loadHeadersFromResource(R.xml.setting_preferences, target);
//    }
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(TAG, "SettingsActivity : onSharedPreferenceChanged : " + key);
        if (key.equals("adding")) {
            Intent intent = new Intent(getApplicationContext(), CloudsConfigureActivity.class);
            startActivity(intent);
        }
        else if(key.equals("endangered_list")) {
            String dataReceived=sharedPreferences.getString("endangered_list",null);
//            if(dataReceived != null) {
//                int num = Integer.getInteger(dataReceived);
//                Toast.makeText(getApplicationContext(), num, Toast.LENGTH_SHORT).show();
//                //TODO calling setCloudDanger in VaultClient
//            }
        }
    }

    /*
    * ActionBar functions
    * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

//    /*
//    * AppCompatDelegate related functions to allow
//    * Material Toolbar to work with Preference Activity. */
//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        getDelegate().onPostCreate(savedInstanceState);
//    }
//    public ActionBar getSupportActionBar() {
//        return getDelegate().getSupportActionBar();
//    }
//    public void setSupportActionBar(@Nullable Toolbar toolbar) {
//        getDelegate().setSupportActionBar(toolbar);
//    }
//
//    @Override
//    public MenuInflater getMenuInflater() {
//        return getDelegate().getMenuInflater();
//    }
//    @Override
//    public void setContentView(@LayoutRes int layoutResID) {
//        getDelegate().setContentView(layoutResID);
//    }
//    @Override
//    public void setContentView(View view) {
//        getDelegate().setContentView(view);
//    }
//    @Override
//    public void setContentView(View view, ViewGroup.LayoutParams params) {
//        getDelegate().setContentView(view, params);
//    }
//    @Override
//    public void addContentView(View view, ViewGroup.LayoutParams params) {
//        getDelegate().addContentView(view, params);
//    }
//    @Override
//    protected void onPostResume() {
//        super.onPostResume();
//        getDelegate().onPostResume();
//    }
//    @Override
//    protected void onTitleChanged(CharSequence title, int color) {
//        super.onTitleChanged(title, color);
//        getDelegate().setTitle(title);
//    }
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        getDelegate().onConfigurationChanged(newConfig);
//    }
//    @Override
//    protected void onStop() {
//        super.onStop();
//        getDelegate().onStop();
//    }
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        getDelegate().onDestroy();
//    }
//    public void invalidateOptionsMenu() {
//        getDelegate().invalidateOptionsMenu();
//    }
//    private AppCompatDelegate getDelegate() {
//        if (mDelegate == null) {
//            mDelegate = AppCompatDelegate.create(this, null);
//        }
//        return mDelegate;
//    }

}
