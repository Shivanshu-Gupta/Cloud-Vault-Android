package com.cloudsecurity.cloudvault;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.cloud.CloudListFragment;
import com.cloudsecurity.cloudvault.cloud.dropbox.DropboxAuthenticator;
import com.cloudsecurity.cloudvault.dropbox.Dropbox;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;

public class CloudVault extends AppCompatActivity implements CloudListFragment.OnFragmentInteractionListener {
    private static final String TAG = "CloudVault";

    VaultClient client;
    boolean mBound;
    private ServiceConnection mConnection;

    private Fragment contentFragment;

    private IntentFilter filter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        Log.v(TAG, "CloudVault : onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_vault);

        CloudSharedPref cloudSharedPref = new CloudSharedPref(this);
        int cloudCount = cloudSharedPref.getCloudCount(this);
        if(cloudCount >= 4) {
            Log.v(TAG, "Enough Clouds for functioning. Setting the content to Files Fragment.");
            contentFragment = new Files();
            setFragmentTitle(R.string.app_name);
            switchContent(contentFragment, Files.ARG_ITEM_ID);
        } else {
            Log.v(TAG, "Not Enough Clouds for functioning. Setting the content to Cloud List Fragment.");
            contentFragment = new CloudListFragment();
            setFragmentTitle(R.string.clouds);
            switchContent(contentFragment, CloudListFragment.ARG_ITEM_ID);
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

        filter = new IntentFilter(Dropbox.LOGGED_IN);
        filter.addAction(Dropbox.LOGGED_OUT);
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCloudsChanged() {
        Log.v(TAG, "CloudVault : onCloudsChanged");
        client.updateClouds();
        client.uploadTable(this);
    }

    @Override
    public void onCloudsDangerChanged() {
        Log.v(TAG, "CloudVault : onCloudsDangerChanged");
        //TODO: update the client's cloudDanger
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

}