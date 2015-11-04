package com.cloudsecurity.cloudvault.cloud;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Shivanshu Gupta on 25-Oct-15.
 */
public class FolderCloud  implements Cloud{
    private static final String TAG = "CloudVault";
    public static final String FOLDERCLOUD = "FOLDERCLOUD";
    private static final String vaultPath = "/CLOUDVAULT/";

    public static final String PATH = "path";

    private String folderPath;

    Context mContext;

    public FolderCloud(Context mContext, String folderPath) {
        this.mContext = mContext;
        this.folderPath = folderPath;
    }

    @Override
    public boolean upload(Context context, String cloudFileName, byte[] data) {
        String filePath = folderPath + vaultPath + cloudFileName;
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "unable to open file for writing: " + filePath);
            e.printStackTrace();
        } catch (IOException e) {
            Log.v(TAG, "Exception while writing to file");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public byte[] download(Context context, String cloudFileName) {
        String filePath = folderPath + vaultPath + cloudFileName;
        try {
            FileInputStream fis = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "");
            e.printStackTrace();
        }
        return new byte[0];
    }
}