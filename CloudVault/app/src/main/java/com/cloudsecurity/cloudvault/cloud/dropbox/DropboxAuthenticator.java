package com.cloudsecurity.cloudvault.cloud.dropbox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.R;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.util.ArrayList;

/**
 * Created by Shivanshu Gupta on 25-Oct-15.
 */
public class DropboxAuthenticator extends Activity {
    private static final String TAG = "CloudVault";

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    private static final String APP_KEY = "jahcg9ypjnokceh";
    private static final String APP_SECRET = "7tgx90ejlj65v12";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    public static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    public static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    public static final String ALREADY_AUTHED_UIDS = "ALREADY_AUTHED_UIDS";

    public static final String ACTION_LOGIN = "com.cloudsecurity.cloudvault.action.LOGIN";
    public static final String ACTION_LOGOUT = "com.cloudsecurity.cloudvault.action.LOGOUT";

    DropboxAPI<AndroidAuthSession> mApi;

    //TODO: dirty hack. Get rid of it.
    private boolean onResumeFirstCall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "DbxAuthenticator : onCreate");
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
        }

        // We create a new AuthSession so that we can use the Dropbox API.
//        AndroidAuthSession session = buildSession();
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        checkAppKeySetup();

        Intent intent = getIntent();
        String desiredUid = intent.getStringExtra(DropboxHandle.UID);
        String[] alreadyAuthedUids = intent.getStringArrayExtra(ALREADY_AUTHED_UIDS);
        Log.v(TAG, "Intent.action: " + intent.getAction());
        if (ACTION_LOGIN.equals(intent.getAction())) {
           if(!mApi.getSession().isLinked()) {
               // Start the remote authentication
               Log.v(TAG, "starting authentication");
               if(desiredUid != null) {
                   mApi.getSession().startOAuth2Authentication(this, desiredUid, alreadyAuthedUids);
               } else {
                   mApi.getSession().startOAuth2Authentication(this, alreadyAuthedUids);
               }
           } else {
               String oauth2AccessToken = session.getOAuth2AccessToken();
               Intent result = new Intent();
               result.putExtra(ACCESS_KEY_NAME, "oauth2:");
               result.putExtra(ACCESS_SECRET_NAME, oauth2AccessToken);
               try {
                   result.putExtra(DropboxHandle.UID, mApi.accountInfo().uid);
               } catch (DropboxException e) {
                   Log.i(TAG, "Unable to get Account Info: " + e.getLocalizedMessage());
                   e.printStackTrace();
               }
               setResult(Activity.RESULT_OK, result);
               finish();
           }
        } else if (ACTION_LOGOUT.equals(intent.getAction())) {
            logOut();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "DbxAuthenticator : onResume");
        super.onResume();
        if(!onResumeFirstCall) {
            AndroidAuthSession session = mApi.getSession();
            Intent result = new Intent();
            // The next part must be inserted in the onResume() method of the
            // activity from which session.startAuthentication() was called, so
            // that Dropbox authentication completes properly.
            if (session.authenticationSuccessful()) {
                try {
                    // Mandatory call to complete the auth
                    session.finishAuthentication();
                    Log.v(TAG, "authentication successful");
                    // Store it locally in our app for later use
                    //instead of storing here, return to store in the common meta data file.
//                    storeAuth(session);
                    String oauth2AccessToken = session.getOAuth2AccessToken();
                    result.putExtra(ACCESS_KEY_NAME, "oauth2:");
                    result.putExtra(ACCESS_SECRET_NAME, oauth2AccessToken);
                    result.putExtra(DropboxHandle.UID, Long.toString(mApi.accountInfo().uid));
                    setResult(Activity.RESULT_OK, result);
                } catch (IllegalStateException e) {
                    showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                    Log.i(TAG, "Error authenticating", e);
                    setResult(0, result);
                } catch (DropboxException e) {
                    Log.i(TAG, "Unable to get Account Info: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            } else {
                setResult(0, result);
            }
            finish();
        } else {
            onResumeFirstCall = false;
        }
    }

    //will not be used
    //TODO: handle logout when multiple accounts logged in
    private void logOut() {
        // Remove credentials from the session
        mApi.getSession().unlink();

        // Clear our stored keys
//        clearKeys();
        // Change UI state to display logged out version
        Intent result = new Intent();
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void checkAppKeySetup() {
        // Check to make sure that we have a valid app key
        if (APP_KEY.startsWith("CHANGE") ||
                APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            finish();
            return;
        }

        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme);
            finish();
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

//    /**
//     * Shows keeping the access keys returned from Trusted Authenticator in a local
//     * store, rather than storing user name & password, and re-authenticating each
//     * time (which is not to be done, ever).
//     */
//    private void loadAuth(AndroidAuthSession session) {
//        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
//        String key = prefs.getString(ACCESS_KEY_NAME, null);
//        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
//        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;
//        session.setOAuth2AccessToken(secret);
//    }
//
//    /**
//     * Shows keeping the access keys returned from Trusted Authenticator in a local
//     * store, rather than storing user name & password, and re-authenticating each
//     * time (which is not to be done, ever).
//     */
//    private void storeAuth(AndroidAuthSession session) {
//        // Store the OAuth 2 access token, if there is one.
//        String oauth2AccessToken = session.getOAuth2AccessToken();
//        if (oauth2AccessToken != null) {
//            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
//            SharedPreferences.Editor edit = prefs.edit();
//            edit.putString(ACCESS_KEY_NAME, "oauth2:");
//            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
//            edit.commit();
//            return;
//        }
//    }
//
//    private void clearKeys() {
//        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
//        SharedPreferences.Editor edit = prefs.edit();
//        edit.clear();
//        edit.commit();
//    }
//
//    private AndroidAuthSession buildSession() {
//        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
//
//        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
//        loadAuth(session);
//        return session;
//    }


    @Override
    protected void onStart() {
        Log.v(TAG, "DbxAuthenticator : onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "DbxAuthenticator : onRestart");
        super.onRestart();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "DbxAuthenticator : onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "DbxAuthenticator : onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "DbxAuthenticator : onDestroy");
        super.onDestroy();
    }
}
