package com.cloudsecurity.cloudvault.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.DatabaseHelper;
import com.cloudsecurity.cloudvault.R;
import com.cloudsecurity.cloudvault.VaultClient;
import com.cloudsecurity.cloudvault.cloud.CloudListFragment;

/**
 * Created by Noman on 11/25/2015.
 * This class is used to add any clouds via AddCloud option in the Settings Tab
 */


public class AddCloud extends AppCompatActivity implements CloudListFragment.OnFragmentInteractionListener {
    private static final String TAG = "CloudVault";

    VaultClient client;
    boolean mBound;
    private ServiceConnection mConnection;

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver receiver = null;
    private IntentFilter filter;

    private Fragment contentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "AddCloud : onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cloud_vault);
        contentFragment = new CloudListFragment();
        setFragmentTitle(R.string.clouds);
        switchContent(contentFragment, CloudListFragment.ARG_ITEM_ID);

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

        mLocalBroadcastManager =  LocalBroadcastManager.getInstance(this);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent !=null) {
                    final String action = intent.getAction();
                    if(action.equals(VaultClient.FILE_CLOUDLISTS_UPDATED)) {
                        // cloudLists have been updated, databases needs to be uploaded
                        client.upload(null);
                    }
                }
            }
        };
        filter = new IntentFilter(VaultClient.FILE_CLOUDLISTS_UPDATED);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "AddCloud : onStart");
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, VaultClient.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "AddCloud : onStop");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cloud_vault, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCloudAdded() {
        // TODO : do whatever might need to be done here
        Log.v(TAG, "AddCloud : onCloudAdded");
        client.updateClouds();
        client.upload(null);
    }

    @Override
    public void onCloudDeleted(String genericName) {
        Log.v(TAG, "AddCloud : onCloudDeleted");
        client.updateClouds();
        client.updateFileCloudLists(genericName);

        //database upload takes place after client bcasts that it's done updatinf cloudlists.
//        client.upload(null);
    }

    @Override
    public void onCloudsDangerChanged() {
        // TODO : do whatever might need to be done here
    }

    public void switchContent(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager
                    .beginTransaction();
            transaction.replace(R.id.content_frame, fragment, tag);
            transaction.commit();
            contentFragment = fragment;
        }
    }

    protected void setFragmentTitle(int resourseId) {
        setTitle(resourseId);
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "AddCloud : onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "AddCloud : onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "AddCloud : onRestart");
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "AddCloud : onDestroy");
        super.onDestroy();
        Intent intent = new Intent(this, VaultClient.class);
        stopService(intent);
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

}