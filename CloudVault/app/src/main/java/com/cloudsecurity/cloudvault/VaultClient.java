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

import com.cloudsecurity.cloudvault.cloud.Cloud;
import com.cloudsecurity.cloudvault.cloud.CloudMeta;
import com.cloudsecurity.cloudvault.cloud.FolderCloud;
import com.cloudsecurity.cloudvault.cloud.dropbox.DropboxHandle;
import com.cloudsecurity.cloudvault.dropbox.Dropbox;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;
import com.cloudsecurity.cloudvault.util.Pair;
import com.cloudsecurity.cloudvault.util.PathManip;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static final String DOWNLOADS_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private final IBinder mBinder = new ClientBinder();
    int cloudNum = 1;
    int cloudDanger = 0; // Cd

    private CloudSharedPref cloudSharedPref;
    private ArrayList<CloudMeta> cloudMetas = new ArrayList<>();
    private ArrayList<Cloud> clouds = new ArrayList<>();

    //check if it should be static, final
    private LocalBroadcastManager mLocalBroadcastManager = null;
    private DatabaseHelper db = null;
    private SQLiteDatabase database;
    private InsertTask insertTask = null;
    private RemoveTask removeTask = null;

//    private Context mContext;
//
//    private String mErrorMsg;
//
//    public VaultClient(Context mContext) {
//        this.mContext = mContext;
//    }
//
//    public VaultClient() {
//        super();
//    }

    @Override
    public void onCreate() {
        Log.v(TAG, "VaultClient : onCreate");
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        cloudSharedPref = new CloudSharedPref(this);
        cloudMetas = cloudSharedPref.getClouds(this);
        Cloud cloud;
        if(cloudMetas != null) {
            for (CloudMeta cloudMeta : cloudMetas) {
                switch (cloudMeta.getName()) {
                    case DropboxHandle.DROPBOX:
                        cloud = new DropboxHandle(this, cloudMeta);
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
        db = DatabaseHelper.getInstance(getApplicationContext());
        database = db.getWritableDatabase();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private Pair<FECParameters, Integer> getParams(long fileSize) {
        // epsilon
        int overHead = 4;
        int symSize = (int) Math.round(Math.sqrt((float) fileSize * 8
                / (float) overHead)); // symbol header length = 8, T = sqrt(D * delta / epsilon)
        int blockCount = (int) Math.ceil((float) fileSize / (float) 30000000);
        FECParameters fecParams = FECParameters.newParameters(fileSize,
                symSize, blockCount);

        int blockSize = (int) Math.ceil((float) fileSize / (float) fecParams.numberOfSourceBlocks());
        int k = (int) Math.ceil((float) blockSize / (float) symSize);

        float gamma = (float) cloudDanger / (float) cloudNum;

        int r = (int) Math.ceil((gamma * k + overHead) / (1 - gamma));

        return Pair.of(fecParams, r);
    }

    public void upload(File file) {
        Log.v(TAG, "VaultClient : upload : " + file);
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

        ContentValues newFile = new ContentValues(2);
        newFile.put(DatabaseHelper.FILENAME, cloudFilePath);
        newFile.put(DatabaseHelper.SIZE, fileSize);
        insertTask = new InsertTask();
        insertTask.execute(newFile);

        UploadTask uploadFile = new UploadTask(file, cloudFilePath);
        uploadFile.execute(this);
    }

    public void download(String cloudFilePath) {
        Log.v(TAG, "VaultClient : download : " + cloudFilePath);
        String writePath;
        long fileSize;
        String[] cols = new String[]{"ROWID AS _id",
                DatabaseHelper.FILENAME,
                DatabaseHelper.SIZE};
        Cursor cursor = db.getReadableDatabase().query(true, DatabaseHelper.TABLE, cols
                , DatabaseHelper.FILENAME + "=?",
                new String[]{cloudFilePath},
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
            //check if need to add a '/'
//            writePath = DOWNLOADS_DIR + '/' + cloudFilePath;
            writePath = DOWNLOADS_DIR + "/SURA/" + cloudFilePath;                   //temporarily

            String fileName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FILENAME));
            fileSize = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.SIZE));
            cloudFilePath = (new PathManip(cloudFilePath)).toCloudFormat();
            DownloadTask downloadFile = new DownloadTask(cloudFilePath, writePath, fileSize);
            downloadFile.execute(this);
        } else {
            Log.e(TAG, "File not found in the database : " + cloudFilePath);
        }
    }

    public void delete(String cloudFilePath) {
        Log.v(TAG, "VaultClient : delete : " + cloudFilePath);
        String[] cols = new String[]{"ROWID AS _id",
                DatabaseHelper.FILENAME,
                DatabaseHelper.SIZE};
        Cursor cursor = db.getReadableDatabase().query(true, DatabaseHelper.TABLE, cols
                , DatabaseHelper.FILENAME + "=?",
                new String[]{cloudFilePath},
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
            String fileName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FILENAME));
            long fileSize = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.SIZE));
            cloudFilePath = (new PathManip(cloudFilePath)).toCloudFormat();
            DeleteTask deleteFile = new DeleteTask(cloudFilePath, fileSize);
            deleteFile.execute(this);
            removeTask = new RemoveTask();
            removeTask.execute(cloudFilePath);
        } else {
            Log.e(TAG, "File not found in the database : " + cloudFilePath);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "VaultClient : onBind");
        return mBinder;
    }

    private class UploadTask extends AsyncTask<Context, Void, Void> {
        File file;
        String cloudFilePath;

        public UploadTask(File file, String cloudFilePath) {
            this.file = file;
            this.cloudFilePath = cloudFilePath;
        }

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
            try {
                byte[] data = readFileToByteArray(file);
                long fileSize = data.length;
                Log.i(TAG, "Uploading: " + file.getPath());
                Pair<FECParameters, Integer> fecparams = getParams(fileSize);
                FECParameters fecParams = fecparams.first;
                int symSize = fecParams.symbolSize();
                int blockSize = (int) Math.ceil((float) fileSize / (float) fecParams.numberOfSourceBlocks());
                int k = (int) Math.ceil((float) blockSize / (float) symSize);
                int r = fecparams.second;

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
                        int cloudID = packetID % cloudNum;
                        dataArrays.get(cloudID).write(packetdata, 0, packetdata.length);
                        packetID++;
                    }

                    int cloudID = 0;
                    for(Cloud cloud: clouds) {
                        cloud.upload(context, blockFileName, dataArrays.get(cloudID).toByteArray());
                        cloudID++;
                    }
                    blockID++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in uploading File " + cloudFilePath, e);
                e.printStackTrace();
            }
            return null;
        }
    }

    private class DownloadTask extends AsyncTask<Context, Void, Void> {
        Long fileSize;
        String cloudFilePath;
        String writePath;

        public DownloadTask(String cloudFilePath, String writePath, long fileSize) {
            this.cloudFilePath = cloudFilePath;
            this.writePath = writePath;
            this.fileSize = fileSize;
        }

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
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
                while (blockID < blockCount) {
                    blockFileName = cloudFilePath + "_" + blockID;
                    for(Cloud cloud : clouds) {
                        blocksData.add(cloud.download(context, blockFileName));
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
            } catch (Exception e) {
                Log.e(TAG, "Exception in downloading " + writePath, e);
            }
            return null;
        }
    }

    private class DeleteTask extends AsyncTask<Context, Void, Void> {
        Long fileSize;
        String cloudFilePath;

        public DeleteTask(String cloudFilePath, long fileSize) {
            this.fileSize = fileSize;
            this.cloudFilePath = cloudFilePath;
        }

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
            Pair<FECParameters, Integer> fecparams = getParams(fileSize);
            FECParameters fecParams = fecparams.first;
            int blockID = 0, blockCount = fecParams.numberOfSourceBlocks();
            String blockFileName;
            try {
                while (blockID < blockCount) {
                    blockFileName = cloudFilePath + "_" + blockID;
                    for(Cloud cloud : clouds) {
                        cloud.delete(context, blockFileName);
                    }
                    blockID++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in deleting " + cloudFilePath, e);
            }
            return null;
        }
    }

    /*
    * An extension of Binder class used to bind to this class.
    * */
    public class ClientBinder extends Binder {
        VaultClient getService() {
            return VaultClient.this;
        }
    }

    abstract private class BaseTask<T> extends AsyncTask<T, Void, Cursor> {
        protected Cursor doQuery() {
            Cursor result =
                    db
                            .getReadableDatabase()
                            .query(DatabaseHelper.TABLE,
                                    new String[]{"ROWID AS _id",
                                            DatabaseHelper.FILENAME,
                                            DatabaseHelper.SIZE},
                                    null, null, null, null, DatabaseHelper.FILENAME);

            result.getCount();

            return (result);
        }
    }

    private class InsertTask extends BaseTask<ContentValues> {
        @Override
        public void onPostExecute(Cursor result) {
            Intent intent = new Intent(FILE_UPLOADED);
            mLocalBroadcastManager.sendBroadcast(intent);
            insertTask = null;
        }

        @Override
        protected Cursor doInBackground(ContentValues... values) {
            database = db.getWritableDatabase();
            database.insert(DatabaseHelper.TABLE, DatabaseHelper.FILENAME, values[0]);
            return (doQuery());
        }
    }

    private class RemoveTask extends BaseTask<String> {
        @Override
        public void onPostExecute(Cursor result) {
            Intent intent = new Intent(FILE_DELETED);
            mLocalBroadcastManager.sendBroadcast(intent);
            removeTask = null;
        }

        @Override
        protected Cursor doInBackground(String... values) {
            database = db.getWritableDatabase();
            database.delete(DatabaseHelper.TABLE, DatabaseHelper.FILENAME + "=?",
                    new String[]{values[0]});
            return (doQuery());
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG,"VaultClient : onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG,"VaultClient : onDestroy");
        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(TAG,"VaultClient : onRebind");
        super.onRebind(intent);
    }
}
