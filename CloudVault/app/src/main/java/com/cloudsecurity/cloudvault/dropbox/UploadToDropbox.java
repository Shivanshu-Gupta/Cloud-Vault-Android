package com.cloudsecurity.cloudvault.dropbox;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Shivanshu Gupta on 19-Sep-15.
 */
public class UploadToDropbox extends AsyncTask<Void, Long, Boolean> {
    private static final String TAG = "CloudVault";

    private boolean isByteArray = false;
    private DropboxAPI<?> mApi;
    private String mPath;
    private File mFile;
    private ByteArrayOutputStream mFileData;

    private long mFileLen;
    private DropboxAPI.UploadRequest mRequest;
    private Context mContext;

    private String mErrorMsg;


    public UploadToDropbox(Context context, DropboxAPI<?> api, String cloudFilePath,
                           File file) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mFileLen = file.length();
        mApi = api;
        mPath = cloudFilePath;
        mFile = file;
        isByteArray = false;
    }

    public UploadToDropbox(Context context, DropboxAPI<?> api, String cloudFilePath,
                           ByteArrayOutputStream data) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mFileLen = data.size();
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
                is = new ByteArrayInputStream(mFileData.toByteArray());
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
