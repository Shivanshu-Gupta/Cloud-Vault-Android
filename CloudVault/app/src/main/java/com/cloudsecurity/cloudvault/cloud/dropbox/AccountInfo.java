package com.cloudsecurity.cloudvault.cloud.dropbox;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

/**
 * Created by cyberLab on 28-11-2015.
 */
public class AccountInfo extends IntentService {
    private static final String TAG = "CloudVault";

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    private static final String APP_KEY = "jahcg9ypjnokceh";
    private static final String APP_SECRET = "7tgx90ejlj65v12";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    public static final String ACTION_FETCH_ACCOUNT = "com.cloudsecurity.cloudvault.cloud.dropbox.action.FETCH_ACCOUNT";
    public static final String FETCH_ACCOUNT_FAILED = "com.cloudsecurity.cloudvault.cloud.dropbox.FETCH_ACCOUNT_FAILED";
    public static final String ACCOUNT_INFO = "com.cloudsecurity.cloudvault.cloud.dropbox.ACCOUNT_INFO";
    public static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    public String ACCESS_SECRET;

    DropboxAPI<AndroidAuthSession> mApi;
    LocalBroadcastManager mLocalBroadCastManager;

    public AccountInfo() {
        super("AccountInfo");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public AccountInfo(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mLocalBroadCastManager = LocalBroadcastManager.getInstance(this);
        ACCESS_SECRET = intent.getStringExtra(ACCESS_SECRET_NAME);
        Log.v(TAG, "AccountInfo : ACCESS_SECRET : " + ACCESS_SECRET);
        Intent infoIntent = null;
        boolean success = false;
        if(setupApi()) {
            switch (intent.getAction()) {
                case ACTION_FETCH_ACCOUNT:
                    try {
                        if(!mApi.getSession().isLinked()) {
                            Log.v(TAG, "still not linked!!!!");
                        }
                        DropboxAPI.Account account = mApi.accountInfo();
                        long uid = account.uid;
                        String email = account.email;
                        Log.v(TAG, "AccountInfo : UID : " + uid);
                        infoIntent = new Intent(ACCOUNT_INFO);
                        infoIntent.putExtra("uid", uid);
                        infoIntent.putExtra("email", email);
                        success = true;
                    } catch (DropboxException e) {
                        e.printStackTrace();
                    }
            }
        }
        if(!success) {
            Log.v(TAG, "Couldn't fetch the Account Info");
            infoIntent = new Intent(FETCH_ACCOUNT_FAILED);
        }
        mLocalBroadCastManager.sendBroadcast(infoIntent);
    }

    private boolean setupApi() {
        Log.v(TAG, "AccountInfo : setupApi");
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair,ACCESS_SECRET);
        mApi = new DropboxAPI<>(session);
        if(!mApi.getSession().isLinked()) {
            Log.v(TAG, "still not linked!!!!");
            return false;
        }
        return true;
    }
}
