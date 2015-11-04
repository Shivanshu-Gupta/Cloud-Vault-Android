package com.cloudsecurity.cloudvault.cloud.dropbox;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.cloud.Cloud;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Shivanshu Gupta on 25-Oct-15.
 */
public class Dropbox extends IntentService {
    private static final String TAG = "CloudVault";
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
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    DropboxAPI<AndroidAuthSession> mApi;
    Context mContext;

    public static final String ACTION_UPLOAD = "com.cloudsecurity.cloudvault.action.UPLOAD";
    public static final String ACTION_DOWNLOAD = "com.cloudsecurity.cloudvault.action.DOWNLOAD";
    public static final String ACTION_DELETE = "com.cloudsecurity.cloudvault.action.DELETE";

    public static final String CLOUDFILEPATH = "com.cloudsecurity.cloudvault.extra.CLOUDFILEPATH";
    private String filePath = null;
    public static final String FILEDATA = "com.cloudsecurity.cloudvault.extra.FILEDATA";
    private byte[] fileData;
    public static final String FILESIZE = "com.cloudsecurity.cloudvault.extra.FILESIZE";
    private long fileSize;

    public Dropbox() {
        super("DropboxHandle");
//        setupApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = this;
        Log.i(TAG, "in the service");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                if(setupApi()) {
                    final String cloudFilePath = intent.getStringExtra(CLOUDFILEPATH);
                    fileData = intent.getByteArrayExtra(FILEDATA);
                    handleActionUpload(vaultPath + cloudFilePath, fileData);
                } else {
                    showToast("Couldn't upload to Dropbox");
                    Log.i(TAG, "Error uploading to Dropbox");
                }
            } else if (ACTION_DOWNLOAD.equals(action)) {
                //NOT SURE IF WOULD USE THIS....

                if(setupApi()) {
                    filePath = intent.getStringExtra(CLOUDFILEPATH);
                    handleActionDownload(vaultPath + filePath);
                } else {
                    showToast("Couldn't download from Dropbox");
                    Log.i(TAG, "Error downloading from Dropbox");
                }
            } else if (ACTION_DELETE.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionDelete(param1, param2);
            }
        } else {
            Log.i(TAG, "Seems the intent is nul...");
        }
    }

    private boolean setupApi() {
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<>(session);
        if(!mApi.getSession().isLinked()) {
            Log.v(TAG, "still not linked!!!!");
            return false;
        }
        return true;
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;
        session.setOAuth2AccessToken(secret);
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    /**
     * Handle action Upload in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpload(String cloudFilePath, byte[] data) {
        Upload upload = new Upload(this, mApi, cloudFilePath, data);
        upload.execute();
    }

    /**
     * Handle action Upload in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDownload(String cloudFilePath) {
        Download download = new Download(this, mApi, cloudFilePath);
        download.execute();
    }

    /**
     * Handle action Upload in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDelete(String param1, String param2) {
        // TODO: Handle action Delete
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    public boolean upload(Context context,String cloudFilePath, byte[] fileData) {
        mContext = context;
        String mErrorMsg = "";
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
        showToast(mErrorMsg);
        return false;
    }

    public byte[] download(Context context, String cloudFilePath) {
        mContext = context;
        String mErrorMsg = "";
        try {
            if(setupApi()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                mApi.getFile(vaultPath + cloudFilePath, null, bos, null);
                fileData = bos.toByteArray();
                return fileData;
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

    public class Upload extends AsyncTask<Void, Long, Boolean> {
        private static final String TAG = "CloudVault";

        private boolean isByteArray = false;
        private DropboxAPI<?> mApi;
        private String mPath;
        private File mFile;
        private byte[] mFileData;

        private long mFileLen;
        private DropboxAPI.UploadRequest mRequest;
        private Context mContext;

        private String mErrorMsg;


        public Upload(Context context, DropboxAPI<?> api, String cloudFilePath,
                      File file) {
            // We set the context this way so we don't accidentally leak activities
            mContext = context.getApplicationContext();
            mFileLen = file.length();
            mApi = api;
            mPath = cloudFilePath;
            mFile = file;
            isByteArray = false;
        }

        public Upload(Context context, DropboxAPI<?> api, String cloudFilePath,
                      byte[] data) {
            // We set the context this way so we don't accidentally leak activities
            mContext = context.getApplicationContext();
            mFileLen = data.length;
            mApi = api;
            mPath = cloudFilePath;
            mFileData = data;
            isByteArray = true;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // By creating a request, we get a handle to the putFile operation,
                // so we can cancel it later if we want to
                InputStream is;
                if(!isByteArray) {
                    is = new FileInputStream(mFile);
                }
                else {
                    is = new ByteArrayInputStream(mFileData);
                }
                Log.v(TAG, "file size: " + mFileLen);
                if(mApi != null) {
                    mRequest = mApi.putFileOverwriteRequest(mPath, is, mFileLen, null);
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
            } catch (FileNotFoundException e) {
            }
            return false;
        }
    }

    public class Download extends AsyncTask<Void, Long, byte[]> {

        private Context mContext;
        private DropboxAPI<?> mApi;
        private String mPath;

        private String mErrorMsg;

        public Download(Context context, DropboxAPI<?> api,
                        String cloudFilePath) {
            // We set the context this way so we don't accidentally leak activities
            mContext = context.getApplicationContext();

            mApi = api;
            mPath = cloudFilePath;
        }

        @Override
        protected byte[] doInBackground(Void... params) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                mApi.getFile(mPath, null, bos, null);
                fileData = bos.toByteArray();
                return fileData;

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
            return null;
        }
    }
}

