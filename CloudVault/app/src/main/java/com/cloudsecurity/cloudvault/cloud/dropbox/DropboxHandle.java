package com.cloudsecurity.cloudvault.cloud.dropbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.cloud.Cloud;
import com.cloudsecurity.cloudvault.cloud.CloudMeta;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by Shivanshu Gupta on 29-Oct-15.
 */
public class DropboxHandle implements Cloud {
    private static final String TAG = "CloudVault";
    public static final String DROPBOX = "DROPBOX";
    private static final String vaultPath = "/CLOUDVAULT/";

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    private static final String APP_KEY = "jahcg9ypjnokceh";
    private static final String APP_SECRET = "7tgx90ejlj65v12";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    private static final String ACCOUNT_PREFS_NAME = "prefs";
    public static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    public static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    public static final String UID = "uid";
    private CloudMeta meta;

    DropboxAPI<AndroidAuthSession> mApi;
    Context mContext;

    public DropboxHandle(Context context) {
        super();
        mContext = context;
        setupApi();
    }

    public DropboxHandle(Context context, CloudMeta meta) {
        super();
        mContext = context;
        this.meta=meta;
        setupApi();
    }

    private boolean setupApi() {
//        AndroidAuthSession session = buildSession();
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AccessTokenPair accessTokenPair = new AccessTokenPair("oauth2", meta.getMeta().get(ACCESS_SECRET_NAME));
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair,accessTokenPair);
        mApi = new DropboxAPI<>(session);
        if(!mApi.getSession().isLinked()) {
            Log.v(TAG, "still not linked!!!!");
            return false;
        }
        return true;
    }

    @Override
    public boolean upload(Context context,String cloudFilePath, byte[] fileData) {
        mContext = context;
        String mErrorMsg = "";
        if(mApi.getSession().isLinked()) {
            try {
                InputStream is;
                is = new ByteArrayInputStream(fileData);

                Log.v(TAG, "file size: " + fileData.length);
                DropboxAPI.UploadRequest mRequest = null;
                setupApi();
                if(mApi != null) {
                    mRequest = mApi.putFileOverwriteRequest(vaultPath + cloudFilePath, is, fileData.length, null);
                } else {
                    Log.v(TAG, "mApi is null!!!!!! :/");
                }

                if (mRequest != null) {
                    mRequest.upload();
                    return true;
                }

            } catch (DropboxUnlinkedException e) {
                // This session wasn't authenticated properly or user unlinked
                mErrorMsg = "This app wasn't authenticated properly.";
            } catch (DropboxFileSizeException e) {
                // File size too big to upload via the API
                mErrorMsg = "This file is too big to upload";
            } catch (DropboxPartialFileException e) {
                // We canceled the operation
                mErrorMsg = "Upload canceled";
            } catch (DropboxServerException e) {
                // Server-side exception.  These are examples of what could happen,
                // but we don't do anything special with them here.
                if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                    // Unauthorized, so we should unlink them.  You may want to
                    // automatically log the user out in this case.
                } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                    // Not allowed to access this
                } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                    // path not found (or if it was the thumbnail, can't be
                    // thumbnailed)
                } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                    // user is over quota
                } else {
                    // Something else
                }
                // This gets the Dropbox error, translated into the user's language
                mErrorMsg = e.body.userError;
                if (mErrorMsg == null) {
                    mErrorMsg = e.body.error;
                }
            } catch (DropboxIOException e) {
                // Happens all the time, probably want to retry automatically.
                mErrorMsg = "Network error.  Try again.";
            } catch (DropboxParseException e) {
                // Probably due to Dropbox server restarting, should retry
                mErrorMsg = "Dropbox error.  Try again.";
            } catch (DropboxException e) {
                // Unknown error
                mErrorMsg = "Unknown error.  Try again.";
            }
        } else {
            mErrorMsg = "No longer linked to the dropbox cloud";
        }
        if(!mErrorMsg.isEmpty()) {
            showToast(mErrorMsg);
        }
        return false;
    }


    @Override
    public byte[] download(Context context, String cloudFileName) {
        mContext = context;
        String mErrorMsg = "";
        try {
            if(setupApi()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                mApi.getFile(vaultPath + cloudFileName, null, bos, null);
                byte [] data = bos.toByteArray();
                return data;
            }
        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Download canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        }
        showToast(mErrorMsg);
        return null;
    }

//    /**
//     * Shows keeping the access keys returned from Trusted Authenticator in a local
//     * store, rather than storing user name & password, and re-authenticating each
//     * time (which is not to be done, ever).
//     */
//    private void loadAuth(AndroidAuthSession session) {
//        SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
//        String key = prefs.getString(ACCESS_KEY_NAME, null);
//        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
//        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;
//        session.setOAuth2AccessToken(secret);
//    }
//
//    private AndroidAuthSession buildSession() {
//        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
//
//        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
//        loadAuth(session);
//        return session;
//    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
