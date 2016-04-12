package com.cloudsecurity.cloudvault;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.cloudsecurity.cloudvault.settings.SettingsActivity;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;

import com.cloudsecurity.cloudvault.setup.SetupOneFragment;
import com.cloudsecurity.cloudvault.setup.SetupTwoFragment;

public class CloudVault extends AppCompatActivity implements SetupOneFragment.OnFragmentInteractionListener,
        SetupTwoFragment.OnFragmentInteractionListener {
    private static final String TAG = "CloudVault";

    /*
    * used only when started for the first time
    * 0 => don't create a new CloudVault
    * 1 => create a new CloudVault
    * */
    boolean mSetupType;

    VaultClient client;
    boolean mBound;
    private ServiceConnection mConnection;

    private Fragment contentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        Log.v(TAG, "CloudVault : onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_vault);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        CloudSharedPref cloudSharedPref = new CloudSharedPref(this);
        int cloudCount = cloudSharedPref.getCloudCount(this);
        if(cloudCount >= 4) {
            Log.v(TAG, "Enough Clouds for functioning. Setting the content to Files Fragment.");
            contentFragment = FilesFragment.newInstance();
            setFragmentTitle(R.string.app_name);
            switchContent(contentFragment, FilesFragment.ARG_ITEM_ID);
        } else {
            Log.v(TAG, "Not Enough Clouds for functioning. Setting the content to SetupOneFragment");
            contentFragment = SetupOneFragment.newInstance();
            setFragmentTitle(R.string.setup_step_one);
            switchContent(contentFragment, SetupOneFragment.ARG_ITEM_ID);
        }

        mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                VaultClient.ClientBinder binder = (VaultClient.ClientBinder) service;
                client = binder.getService();
                mBound = true;
            }

            @Override
             public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "CloudVault : onStart");
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, VaultClient.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "CloudVault : onStop");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "CloudVault ; onDestroy");
        super.onDestroy();
        Intent intent = new Intent(this, VaultClient.class);
        stopService(intent);
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cloud_vault, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent myIntent = new Intent (this, SettingsActivity.class);
                startActivity(myIntent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void switchContent(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager
                    .beginTransaction();
            transaction.replace(R.id.content_frame, fragment, tag);
            //Only FavoriteListFragment is added to the back stack.
//            if (!(fragment instanceof ProductListFragment)) {
//                transaction.addToBackStack(tag);
//            }
            transaction.commit();
            contentFragment = fragment;
        }
    }

    protected void setFragmentTitle(int resourseId) {
        setTitle(resourseId);
//        getActionBar().setTitle(resourseId);

    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "CloudVault : onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "CloudVault : onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "CloudVault : onRestart");
        super.onRestart();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*
    * Called by SetupOneFragment when user has selected the install type.
    * */
    @Override
    public void onInstallTypeSelected(boolean create) {
        Log.v(TAG, "Install type selected. create = " + create);
        mSetupType = create;
        contentFragment = SetupTwoFragment.newInstance(create);
        setFragmentTitle(R.string.setup_step_two);
        switchContent(contentFragment, SetupTwoFragment.ARG_ITEM_ID);
    }

    @Override
    public void onStepTwoCompleted() {
        Log.v(TAG, "Clouds Added. Finishing CloudVault Setup.");
        client.updateClouds();
        if(mSetupType) {
            // new CloudVault needs to be created - need to upload the table
            // upload when passed null as a parameter just uploads the table.
            client.upload(null);
        } else {
            // connect to existing CloudVault - download the table from existing CloudVault
            // download when passed null as a parameter just downloads the table.
            client.download(null);
        }
        Log.v(TAG, "CloudVault Setup Complete. Switching to Files Fragment.");
        contentFragment = FilesFragment.newInstance();
        setFragmentTitle(R.string.app_name);
        switchContent(contentFragment, FilesFragment.ARG_ITEM_ID);
    }

}