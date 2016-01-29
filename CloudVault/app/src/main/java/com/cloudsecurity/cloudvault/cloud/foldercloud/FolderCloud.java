package com.cloudsecurity.cloudvault.cloud.foldercloud;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cloudsecurity.cloudvault.cloud.Cloud;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Shivanshu Gupta on 25-Oct-15.
 */
public class FolderCloud  implements Cloud {
    private static final String TAG = "CloudVault";
    public static final String FOLDERCLOUD = "FOLDERCLOUD";

    public static final String PATH = "path";

    private String folderPath;

    Context mContext;

    public FolderCloud(Context mContext, String folderPath) {
        this.mContext = mContext;
        this.folderPath = folderPath + "/";
    }

    @Override
    public boolean upload(Context context, String cloudFileName, byte[] data) {
        String filePath = folderPath + cloudFileName;
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "unable to open file for writing: " + filePath);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Exception while writing to file");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public byte[] download(Context context, String cloudFileName) {
        String filePath = folderPath + cloudFileName;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            int bytes_read;
            while(-1 != (bytes_read = fis.read(buffer, 0, buffer.length))) {
                bos.write(buffer, 0, bytes_read);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FolderCloud (" + folderPath + ") : File to download not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "FolderCloud (" + folderPath + ") : Error downloading file");
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public boolean delete(Context context, String cloudFileName) {
        String filePath = folderPath + cloudFileName;
        File file = new File(filePath);
        return file.delete();
    }
}