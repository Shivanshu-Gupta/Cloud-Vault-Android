package com.cloudsecurity.cloudvault.cloud.dropbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.CloudVault;
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
public class Dropbox implements Cloud {
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

    public static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    public static final String UID = "com.cloudsecurity.cloudvault.cloud.dropbox.UID";
    private CloudMeta meta;

    DropboxAPI<AndroidAuthSession> mApi;
    Context mContext;

    public Dropbox(Context context) {
        super();
        mContext = context;
        setupApi();
    }

    public Dropbox(Context context, CloudMeta meta) {
        super();
        mContext = context;
        this.meta=meta;
        setupApi();
    }

    private boolean setupApi() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        String secret = meta.getMeta().get(ACCESS_SECRET_NAME);
//        AccessTokenPair accessTokenPair = new AccessTokenPair("oauth2", secret);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair,secret);
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

            } catch (Exception e) {
              //TODO : remove this catch
              e.printStackTrace();
            }
//            catch (DropboxUnlinkedException e) {
//                // This session wasn't authenticated properly or user unlinked
//                mErrorMsg = "This app wasn't authenticated properly.";
//            } catch (DropboxFileSizeException e) {
//                // File size too big to upload via the API
//                mErrorMsg = "This file is too big to upload";
//            } catch (DropboxPartialFileException e) {
//                // We canceled the operation
//                mErrorMsg = "Upload canceled";
//            } catch (DropboxServerException e) {
//                // Server-side exception.  These are examples of what could happen,
//                // but we don't do anything special with them here.
//                if (e.error == DropboxServerException._401_UNAUTHORIZED) {
//                    // Unauthorized, so we should unlink them.  You may want to
//                    // automatically log the user out in this case.
//                } else if (e.error == DropboxServerException._403_FORBIDDEN) {
//                    // Not allowed to access this
//                } else if (e.error == DropboxServerException._404_NOT_FOUND) {
//                    // path not found (or if it was the thumbnail, can't be
//                    // thumbnailed)
//                } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
//                    // user is over quota
//                } else {
//                    // Something else
//                }
//                // This gets the Dropbox error, translated into the user's language
//                mErrorMsg = e.body.userError;
//                if (mErrorMsg == null) {
//                    mErrorMsg = e.body.error;
//                }
//            } catch (DropboxIOException e) {
//                // Happens all the time, probably want to retry automatically.
//                mErrorMsg = "Network error.  Try again.";
//            } catch (DropboxParseException e) {
//                // Probably due to Dropbox server restarting, should retry
//                mErrorMsg = "Dropbox error.  Try again.";
//            } catch (DropboxException e) {
//                // Unknown error
//                mErrorMsg = "Unknown error.  Try again.";
//            }
        } else {
            mErrorMsg = "No longer linked to the dropbox cloud";
        }
//        if(!mErrorMsg.isEmpty()) {
//            showToast(mErrorMsg);
//        }
        Log.v(TAG, "Dropbox : upload : " + mErrorMsg);
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
//        showToast(mErrorMsg);
        Log.v(TAG, "Dropbox : download : " + mErrorMsg);
        return new byte[0];
    }

    @Override
    public boolean delete(Context context, String cloudFileName) {
        return false;
    }

    //won't work here as it is not th UI thread
    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
