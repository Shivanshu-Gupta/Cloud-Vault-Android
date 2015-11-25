package com.cloudsecurity.cloudvault.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.Files;
import com.cloudsecurity.cloudvault.R;
import com.cloudsecurity.cloudvault.VaultClient;
import com.cloudsecurity.cloudvault.cloud.CloudListFragment;
import com.cloudsecurity.cloudvault.dropbox.Dropbox;

/**
 * Created by Noman on 11/25/2015.
 * This class is used to add any clouds via AddCloud option in the Settings Tab
 */


public class AddCloud extends AppCompatActivity {
    private static final String TAG = "CloudVault";
    private Fragment contentFragment;
    private IntentFilter filter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "AddCloud : onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_vault);
        contentFragment = new CloudListFragment();
        setFragmentTitle(R.string.clouds);
        switchContent(contentFragment, CloudListFragment.ARG_ITEM_ID);
//        filter = new IntentFilter(Dropbox.LOGGED_IN);
//        filter.addAction(Dropbox.LOGGED_OUT);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "AddCloud : onStart");
        super.onStart();
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
    }
}