package com.cloudsecurity.cloudvault;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.cloud.Cloud;
import com.cloudsecurity.cloudvault.cloud.CloudMeta;
import com.cloudsecurity.cloudvault.cloud.foldercloud.FolderCloud;
import com.cloudsecurity.cloudvault.cloud.dropbox.Dropbox;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;
import com.cloudsecurity.cloudvault.util.Pair;
import com.cloudsecurity.cloudvault.util.PathManip;
import com.google.gson.Gson;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import static org.apache.commons.io.FileUtils.readFileToByteArray;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class VaultClient extends Service {
    private static final String TAG = "CloudVault";
    public static final String FILE_UPLOADED = "com.cloudsecurity.cloudvault.action.FILE_UPLOADED";
    public static final String FILE_DELETED = "com.cloudsecurity.cloudvault.action.FILE_DELETED";
    public static final String FILES_DB_SYNCED = "com.cloudsecurity.cloudvault.action.FILES_DB_SYNCED";
    public static final String FILE_CLOUDLISTS_UPDATED = "com.cloudsecurity.cloudvault.action.FILE_CLOUDLISTS_UPDATED";
    public static final String DOWNLOADS_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

    public static final String DB_META = "dbmeta.txt";
//    public final String DB_META_PATH = this.getFilesDir() + "dbmeta.txt";

    private final IBinder mBinder = new ClientBinder();
    int cloudNum;
    int cloudDanger = 1; // Cd

    private CloudSharedPref cloudSharedPref;
    private ArrayList<CloudMeta> cloudMetas = new ArrayList<>();
    private ArrayList<Cloud> clouds = new ArrayList<>();

    //check if it should be static, final
    private LocalBroadcastManager mLocalBroadcastManager = null;
    private DatabaseHelper db = null;
    private SQLiteDatabase database;

    //MD5 hashes are 128 bit or 16 bytes long
    byte[] localDBHash = new byte[16];
    long localDBSize = -1;

    @Override
    public void onCreate() {
        Log.v(TAG, "VaultClient : onCreate");
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        cloudSharedPref = new CloudSharedPref(this);
        updateClouds();
        db = DatabaseHelper.getInstance(getApplicationContext());
        database = db.getWritableDatabase();
    }

    public void updateClouds() {
        cloudMetas.clear();
        clouds.clear();
        cloudMetas = cloudSharedPref.getClouds(this);
        Cloud cloud;
        if (cloudMetas != null) {
            for (CloudMeta cloudMeta : cloudMetas) {
                switch (cloudMeta.getName()) {
                    case Dropbox.DROPBOX:
                        cloud = new Dropbox(this, cloudMeta);
                        clouds.add(cloud);
                        break;
                    case FolderCloud.FOLDERCLOUD:
                        cloud = new FolderCloud(this, cloudMeta.getMeta().get(FolderCloud.PATH));
                        clouds.add(cloud);
                        break;
                }
            }
        }
        cloudNum = clouds.size();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private Pair<FECParameters, Integer> getParams(long fileSize) {
        // epsilon
        Log.v(TAG, "VaultClient : getParams : fileSize : " + fileSize);
        int overHead = 4;
        int symSize = (int) Math.round(Math.sqrt((float) fileSize * 8
                / (float) overHead)); // symbol header length = 8, T = sqrt(D * delta / epsilon)
        int blockCount = (int) Math.ceil((float) fileSize / (float) 3000000);
        FECParameters fecParams = FECParameters.newParameters(fileSize,
                symSize, blockCount);
        if (fecParams == null) {
            Log.v(TAG, "VaultClient : getParams : ahhhm : This is not happening");
        }
        int blockSize = (int) Math.ceil((float) fileSize / (float) fecParams.numberOfSourceBlocks());
        int k = (int) Math.ceil((float) blockSize / (float) symSize);

        Log.v(TAG, "VaultClient : getParams : cloudDanger : " + cloudDanger);
        Log.v(TAG, "VaultClient : getParams : cloudNum : " + cloudNum);
        float gamma = (float) cloudDanger / (float) cloudNum;

        int r = (int) Math.ceil((gamma * k + overHead) / (1 - gamma));

        return Pair.of(fecParams, r);
    }

    public void upload(File file) {
        if (file != null) {
            Log.v(TAG, "VaultClient : upload : " + file);
        }
        UploadTask uploadTask = new UploadTask(file);
        uploadTask.execute(this);
    }

    public void download(String cloudFilePath) {
        if (cloudFilePath != null) {
            Log.v(TAG, "VaultClient : download : " + cloudFilePath);
        }
        DownloadTask downloadTask = new DownloadTask(cloudFilePath);
        downloadTask.execute(this);
    }

    public void delete(String cloudFilePath) {
        Log.v(TAG, "VaultClient : delete : " + cloudFilePath);
        DeleteTask deleteTask = new DeleteTask(cloudFilePath);
        deleteTask.execute(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "VaultClient : onBind");
        return mBinder;
    }

    private class UploadTask extends AsyncTask<Context, Void, Void> {
        File file;

        public UploadTask(File file) {
            this.file = file;
        }

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
            if (file == null) {
                //if the file is null then just upload the file database.
                uploadTable(context);
            } else {
                String uploadPath = "";
                String cloudFilePath;
                String localFileName = file.getName();

                //currently as only single files are being uploaded directly to the root directory, uploadPath will be empty.
                if (uploadPath.length() > 0) {
                    cloudFilePath = uploadPath + "/" + localFileName;
                } else {
                    cloudFilePath = localFileName;
                }

                //change the '/' in the cloudFilePath to '$'
                //no effect as of now as only one level in the paths as of now.
                cloudFilePath = (new PathManip(cloudFilePath)).toCloudFormat();
                long fileSize = file.length();
                downloadTable(context);

                String[] cloudsUsed = uploadFile(context, file, cloudFilePath);
                Log.v(TAG, "cloudsUsed: " + cloudsUsed);
                Gson gson = new Gson();
                String cloudListString = gson.toJson(cloudsUsed);
                int minClouds = cloudsUsed.length - cloudDanger;
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                ContentValues newFile = new ContentValues(4);
                newFile.put(DatabaseHelper.FILENAME, cloudFilePath);
                newFile.put(DatabaseHelper.SIZE, fileSize);
                newFile.put(DatabaseHelper.CLOUDLIST, cloudListString);
                newFile.put(DatabaseHelper.MINCLOUDS, minClouds);
                newFile.put(DatabaseHelper.TIMESTAMP, timestamp.toString());

                insertRecord(newFile);
                uploadTable(context);
            }
            return null;
        }
    }

    private String[] uploadFile(Context context, File file, String cloudFilePath) {
        HashSet<String> cloudsUsed = new HashSet<>();
        try {
            byte[] data = readFileToByteArray(file);
            long fileSize = data.length;
            Log.i(TAG, "VaultClient : UploadTask : " + file.getPath());
            Pair<FECParameters, Integer> fecparams = getParams(fileSize);
            FECParameters fecParams = fecparams.first;
            int symSize = fecParams.symbolSize();
            int blockSize = (int) Math.ceil((float) fileSize / (float) fecParams.numberOfSourceBlocks());
            int k = (int) Math.ceil((float) blockSize / (float) symSize);
            int r = fecparams.second;


            Log.v(TAG, "VaultClient : getParams : symSize : " + symSize);
            Log.v(TAG, "VaultClient : getParams : k : " + k);
            Log.v(TAG, "VaultClient : getParams : r : " + r);

            ArrayDataEncoder dataEncoder = OpenRQ.newEncoder(data, fecParams);

            int packetID = 0, blockID = 0;
            byte[] packetdata;
            Iterable<SourceBlockEncoder> srcBlkEncoders = dataEncoder
                    .sourceBlockIterable();
            String blockFileName;
            for (SourceBlockEncoder srcBlkEnc : srcBlkEncoders) {
                blockFileName = cloudFilePath + "_" + blockID;
                ArrayList<ByteArrayOutputStream> dataArrays = new ArrayList<>(
                        cloudNum);

                for (int i = 0; i < cloudNum; i++) {
                    int blockDataLength = (k + r) * (symSize + 8);
                    dataArrays.add(new ByteArrayOutputStream(blockDataLength) {
                    });
                }

                // using only repair packets and no source packets
                Iterable<EncodingPacket> repPackets = srcBlkEnc
                        .repairPacketsIterable(k + r);
                for (EncodingPacket repPack : repPackets) {
                    packetdata = repPack.asArray();
                    int idx = packetID % cloudNum;
                    dataArrays.get(idx).write(packetdata, 0, packetdata.length);
                    packetID++;
                }

                int idx = 0;
                CloudMeta cloudMeta;
                for (Cloud cloud : clouds) {
                    cloudMeta = cloudMetas.get(idx);
                    try{
                        cloud.upload(context, blockFileName, dataArrays.get(idx).toByteArray());
                        cloudsUsed.add(cloudMeta.getGenericName());
                    } catch(Exception e) {
                        Log.e(TAG, "Exception while uploading File " + cloudFilePath + " to " +
                                cloudMeta.getGenericName());
                    }
                    idx++;
                }
                blockID++;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in uploading File " + cloudFilePath, e);
            e.printStackTrace();
        }
        Log.v(TAG, "Number of clouds used: " + cloudsUsed.size());
        return cloudsUsed.toArray(new String[cloudsUsed.size()]);
    }

    private class DownloadTask extends AsyncTask<Context, Void, Void> {
        String cloudFilePath;

        public DownloadTask(String cloudFilePath) {
            this.cloudFilePath = cloudFilePath;
        }

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
            if (cloudFilePath == null) {
                //if the cloudFilePath is null, use it to just download the files database.
                downloadTable(context);
                Intent intent = new Intent(FILES_DB_SYNCED);
                mLocalBroadcastManager.sendBroadcast(intent);
            } else {
                String writePath;
                downloadTable(context);
                String[] cols = new String[]{"ROWID AS _id",
                        DatabaseHelper.SIZE,
                        DatabaseHelper.CLOUDLIST,
                        DatabaseHelper.MINCLOUDS};
                Cursor cursor = db.getReadableDatabase().query(true, DatabaseHelper.FILES_TABLE,
                        cols, DatabaseHelper.FILENAME + "=?",
                        new String[]{cloudFilePath},
                        null, null, null, null);

                if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                    //check if need to add a '/'
//            writePath = DOWNLOADS_DIR + '/' + cloudFilePath;
                    writePath = DOWNLOADS_DIR + "/SURA/" + cloudFilePath;                   //temporarily

                    long fileSize = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.SIZE));
                    String cloudList = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CLOUDLIST));
                    int minClouds = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.MINCLOUDS));
                    cloudFilePath = (new PathManip(cloudFilePath)).toCloudFormat();
                    //TODO : use cloudList and minClouds!!
                    downloadFile(context, cloudFilePath, writePath, fileSize);
                } else {
                    //may happen that the file is not there in the db as it's is being downloaded fresh
                    //TODO : tell the user that the file wasn't found.
                    Log.e(TAG, "File not found in the database : " + cloudFilePath);
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            showToast("File Downloaded");
        }

    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    private void downloadFile(Context context, String cloudFilePath, String writePath, long fileSize) {
        Pair<FECParameters, Integer> fecparams = getParams(fileSize);
        FECParameters fecParams = fecparams.first;
        int symSize = fecParams.symbolSize();
        int blockID = 0, blockCount = fecParams.numberOfSourceBlocks();
        int packetID, packetCount;
        int packetlength = symSize + 8;
        String blockFileName;
        try {
            ArrayList<byte[]> blocksData = new ArrayList<>();
            byte[] packetdata;
            ArrayDataDecoder dataDecoder = OpenRQ.newDecoder(fecParams, 3);

            // reading in all the packets into a byte[][]
            List<byte[]> packetList = new ArrayList<>();
            CloudMeta cloudMeta;
            while (blockID < blockCount) {
                blockFileName = cloudFilePath + "_" + blockID;
                int idx = 0;
                for (Cloud cloud : clouds) {
                    cloudMeta = cloudMetas.get(idx);
                    try {
                        blocksData.add(cloud.download(context, blockFileName));
                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't download " + writePath + " from " +
                                cloudMeta.getGenericName(), e);
                    }
                    idx++;
                }
                for (byte[] blockData : blocksData) {
                    packetCount = blockData.length / packetlength;
                    for (int j = 0; j < packetCount; j++) {
                        packetdata = Arrays.copyOfRange(blockData, j
                                * packetlength, (j + 1) * packetlength);
                        packetList.add(packetdata);
                    }
                }

                blockID++;
            }
            Log.i(TAG, "packets have been downloaded!");
            packetID = 0;
            while (!dataDecoder.isDataDecoded() && packetID < packetList.size()) {
                byte[] packet = packetList.get(packetID);
                EncodingPacket encPack = dataDecoder.parsePacket(packet, true)
                        .value();
                int sbn = encPack.sourceBlockNumber();
                SourceBlockDecoder srcBlkDec = dataDecoder.sourceBlock(sbn);
                srcBlkDec.putEncodingPacket(encPack);
                packetID++;
            }

            byte dataNew[] = dataDecoder.dataArray();
            FileOutputStream fos = new FileOutputStream(writePath);
            fos.write(dataNew);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception in downloading " + writePath, e);
        }
    }

    private class DeleteTask extends AsyncTask<Context, Void, Void> {
        String cloudFilePath;

        public DeleteTask(String cloudFilePath) {
            this.cloudFilePath = cloudFilePath;
        }

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
            downloadTable(context);
            String[] cols = new String[]{"ROWID AS _id",
                    DatabaseHelper.SIZE,
                    DatabaseHelper.CLOUDLIST};
            Cursor cursor = db.getReadableDatabase().query(true, DatabaseHelper.FILES_TABLE, cols
                    , DatabaseHelper.FILENAME + "=?",
                    new String[]{cloudFilePath},
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                long fileSize = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.SIZE));
                String cloudList =cursor.getString((cursor.getColumnIndex(DatabaseHelper.CLOUDLIST)));
                cloudFilePath = (new PathManip(cloudFilePath)).toCloudFormat();
                removeRecord(cloudFilePath);
                //TODO : use CloudList!!!
                deleteFile(context, cloudFilePath, fileSize);
                uploadTable(context);
            } else {
                Log.e(TAG, "File not found in the database : " + cloudFilePath);
            }
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
    }

    private void deleteFile(Context context, String cloudFilePath, long fileSize) {
        Pair<FECParameters, Integer> fecparams = getParams(fileSize);
        FECParameters fecParams = fecparams.first;
        int blockID = 0, blockCount = fecParams.numberOfSourceBlocks();
        String blockFileName;
        try {
            while (blockID < blockCount) {
                blockFileName = cloudFilePath + "_" + blockID;
                int idx = 0;
                CloudMeta cloudMeta;
                for (Cloud cloud : clouds) {
                    cloudMeta = cloudMetas.get(idx);
                    try{
                        cloud.delete(context, blockFileName);
                    } catch(Exception e) {
                        Log.v(TAG, "Couldn't delete " + cloudFilePath + " from " +
                                cloudMeta.getGenericName(), e);
                    }
                    idx++;
                }
                blockID++;
            }
        } catch (Exception e) {
            //may happen that the file is not there in the db as it's is being downloaded fresh
            //TODO : tell the user that the file wasn't found.
            Log.e(TAG, "Exception in deleting " + cloudFilePath, e);
        }
    }

    public void uploadTable(Context context) {
        Log.v(TAG, "VaultClient : uploadTable");
        db.close();
        byte[] dbData;
        String DB_NAME = DatabaseHelper.DATABASE_NAME, DB_PATH;
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            DB_PATH = this.getApplicationInfo().dataDir + "/databases/" + DB_NAME;
        } else {
            DB_PATH = this.getFilesDir() + this.getPackageName() + "/databases/" + DB_NAME;
        }
        try {
            FileInputStream fis = new FileInputStream(DB_PATH);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytes_read;
            while (-1 != (bytes_read = fis.read(buffer, 0, buffer.length))) {
                bos.write(buffer, 0, bytes_read);
            }
            dbData = bos.toByteArray();
            //TODO: dbSize should be long - FIX!
            long dbSize = dbData.length;
            //get the 128-bit MD5 hash
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(dbData);
            byte[] dbHash = digester.digest();      //16 bytes

            bos.reset();

            // wrap the Bos with a Dos so that size may be written to it.
            DataOutputStream dos = new DataOutputStream(bos);
            dos.write(dbHash);
            dos.writeLong(dbSize);
            byte[] dbMetaData = bos.toByteArray();

            updateDBMetaFile(dbHash, dbSize);
            tinyUploadFile(context, dbMetaData, DB_META);

            localDBSize = dbSize;
            localDBHash = dbHash;
            //TODO: change this tiny upload and download to regular
            uploadFile(context, new File(DB_PATH), DB_NAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "VaultClient : uploadTable : database file not found");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "VaultClient : uploadTable : database file could not be read");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void downloadTable(Context context) {
        Log.v(TAG, "VaultClient : downloadTable");
        boolean databaseChanged = false;

        String DB_META_PATH, DB_PATH;
        String DB_NAME = DatabaseHelper.DATABASE_NAME;

        if (android.os.Build.VERSION.SDK_INT >= 17) {
            DB_META_PATH = this.getApplicationInfo().dataDir + "/" + DB_META;
            DB_PATH = this.getApplicationInfo().dataDir + "/databases/" + "vault.db";
//            DB_PATH = this.getDatabasePath(DB_NAME).getPath();
//            DB_PATH = Environment.DIRECTORY_DOWNLOADS + "/" + DB_NAME;
        } else {
            DB_META_PATH = this.getFilesDir() + "/" + DB_META;
            DB_PATH = this.getFilesDir() + this.getPackageName() + "/databases/" + DB_NAME;
        }
        Log.v(TAG, "VaultClient : DBPATH : " + DB_PATH);

        //read the database size and hash from the file only if they haven't been reinitialised.
        if(localDBSize == -1) {
            try {
                DataInputStream in = new DataInputStream((new FileInputStream(DB_META_PATH)));
                in.read(localDBHash, 0, localDBHash.length);
                localDBSize = in.readLong();
                in.close();
            } catch (IOException e) {
                Log.w(TAG, "VaultClient : downloadTable : could not open existing dbMeta.txt: " + e.getLocalizedMessage());
//                localDBSize = -1;
            } catch (Exception e) {
                Log.e(TAG, "VaultClient : downloadTable : could not get local db meta");
//                localDBSize = -1;
            }
        }

        tinyDownloadFile(context, DB_META, DB_META_PATH);

        try {
            DataInputStream in = new DataInputStream(new FileInputStream(
                    DB_META_PATH));
            byte[] downloadedDBHash = new byte[16];
            in.read(downloadedDBHash, 0, downloadedDBHash.length);

            if (!Arrays.equals(localDBHash, downloadedDBHash) || localDBSize == -1) {
                //database changed if hashes mismatch or the db file hasn't been created yet.
                databaseChanged = true;
            }
            long downloadedDBSize = in.readLong();
            Log.i(TAG, "VaultClient : downloadTable : Local Files DB: Size=" + localDBSize + " Hash="
                    + Arrays.toString(localDBHash));
            Log.i(TAG, "VaultClient : downloadTable : Files DB on cloud: Size=" + downloadedDBSize + " Hash="
                    + Arrays.toString(downloadedDBHash));
            if (databaseChanged) {
                Log.v(TAG, "table hash mismatch. downloading database.");
                database = db.getWritableDatabase();
                database.close();
                
                //TODO: change this tiny upload and download to regular
                downloadFile(context, DB_NAME, DB_PATH, downloadedDBSize);

                localDBSize = downloadedDBSize;
                localDBHash = downloadedDBHash;
            }
            in.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException while downloading table. ");
            e.printStackTrace();
        }
    }

    private boolean updateDBMetaFile(byte[] dbHash, long dbSize) {
        Log.i(TAG, "VaultClient : updateDBMetaFile : Files DB: Size=" + dbSize + " Hash="
                + Arrays.toString(dbHash));
        try {
            String DB_META_PATH;
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                DB_META_PATH = this.getApplicationInfo().dataDir + "/" + DB_META;
            } else {
                DB_META_PATH = this.getFilesDir() + "/" + DB_META;
            }
            Log.v(TAG, DB_META_PATH);
            FileOutputStream fout = new FileOutputStream(DB_META_PATH);
            DataOutputStream dout = new DataOutputStream(fout);
            dout.write(dbHash);
            dout.writeLong(dbSize);

            dout.flush();
            fout.flush();
            dout.close();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void sync() {
        //calling download with null just downloads the table.
        download(null);
    }

    private void tinyUploadFile(Context context, byte[] data, String cloudFilePath) {
        try {
            Log.i(TAG, "VaultClient : TinyUploadTask : " + cloudFilePath);
            int idx = 0;
            for (Cloud cloud : clouds) {
                try{
                    cloud.upload(context, cloudFilePath, data);
                } catch (Exception e) {
                    Log.i(TAG, "Couldn't upload " + cloudFilePath + " to " + cloudMetas.get(idx).getGenericName());
                }
                idx++;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in uploading File " + cloudFilePath, e);
            e.printStackTrace();
        }
    }

    private void tinyDownloadFile(Context context, String cloudFilePath, String writePath) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(writePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "VaultClient : TinyDownloadTask : unable to open file for writing");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (Cloud cloud : clouds) {
            try {
                bos.write(cloud.download(context, cloudFilePath));
                break;
            } catch (IOException e) {
                bos.reset();
//                    e.printStackTrace();
            }
        }
        //TODO : check if just checking the number of bytes written is enough
        if (bos.size() > 0 && fos != null) {
            try {
                fos.write(bos.toByteArray());
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "VaultClient : TinyDownloadTask : unable to write file " + cloudFilePath);
            }
        } else {
            Log.e(TAG, "VaultClient : TinyDownloadTask : couldn't download file " + cloudFilePath);
        }
    }

    public void updateFileCloudLists(String genericName) {
        new FileCloudsListsUpdateTask().execute(genericName);
    }

    /*
    * An extension of Binder class used to bind to this class.
    * */
    public class ClientBinder extends Binder {
        public VaultClient getService() {
            return VaultClient.this;
        }
    }

    private void insertRecord(ContentValues... values) {
        database = db.getWritableDatabase();
        database.insert(DatabaseHelper.FILES_TABLE, DatabaseHelper.FILENAME, values[0]);
        Intent intent = new Intent(FILE_UPLOADED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void removeRecord(String... values) {
        database = db.getWritableDatabase();
        database.delete(DatabaseHelper.FILES_TABLE, DatabaseHelper.FILENAME + "=?",
                new String[]{values[0]});
        Intent intent = new Intent(FILE_DELETED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private class FileCloudsListsUpdateTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i(TAG, "Files' cloudLists updated, sending out bcast");
            Intent intent = new Intent(FILE_CLOUDLISTS_UPDATED);
            mLocalBroadcastManager.sendBroadcast(intent);
        }

        @Override
        protected Void doInBackground(String... params) {
            Cursor cur = db.getWritableDatabase().rawQuery("SELECT * FROM " + DatabaseHelper.FILES_TABLE, null);
            HashSet<String> cloudsUsed = new HashSet<>();
            String[] cloudsUsedList;
            ContentValues updateFile = new ContentValues(1);
            String fileName;
            Gson gson = new Gson();
            if (cur != null) {
                if (cur.moveToFirst()) {
                    do {
                        fileName = cur.getString(cur.getColumnIndex(DatabaseHelper.FILENAME));
                        cloudsUsedList = gson.fromJson(cur.getString(cur.getColumnIndex(DatabaseHelper.CLOUDLIST)), String[].class);
                        cloudsUsed.addAll(Arrays.asList(cloudsUsedList));
                        cloudsUsed.remove(params[0]);
                        cloudsUsedList = cloudsUsed.toArray(new String[cloudsUsed.size()]);
                        updateFile.put(DatabaseHelper.CLOUDLIST, gson.toJson(cloudsUsedList));
                        db.getWritableDatabase().update(DatabaseHelper.FILES_TABLE, updateFile,
                                DatabaseHelper.FILENAME + " = ?", new String[]{fileName});
                    } while (cur.moveToNext());
                }
            }
            return null;
        }
    }

    abstract private class BaseTask<T> extends AsyncTask<T, Void, Cursor> {
        protected Cursor doQuery() {
            Cursor result =
                    db
                            .getReadableDatabase()
                            .query(DatabaseHelper.FILES_TABLE,
                                    new String[]{"ROWID AS _id",
                                            DatabaseHelper.FILENAME,
                                            DatabaseHelper.SIZE},
                                    null, null, null, null, DatabaseHelper.FILENAME);

            result.getCount();

            return (result);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "VaultClient : onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "VaultClient : onDestroy");
        super.onDestroy();
        db.close();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(TAG, "VaultClient : onRebind");
        super.onRebind(intent);
    }

    void setCloudDanger(int x)
    {
        cloudDanger = x;
    }
}
