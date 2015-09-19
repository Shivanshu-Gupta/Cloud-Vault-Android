package com.cloudsecurity.cloudvault;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.CursorAdapter;

import com.cloudsecurity.cloudvault.util.Pair;
import com.cloudsecurity.cloudvault.dropbox.UploadToDropbox;
import com.dropbox.client2.DropboxAPI;

import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import static org.apache.commons.io.FileUtils.readFileToByteArray;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class VaultClient extends Service {
    private static final String TAG = "CloudVault";
    public static final String INTENT_FILE_UPLOADED = "com.cloudsecurity.cloudvault.action.FILE_UPLOADED";

    //check if it should be static, final
    private LocalBroadcastManager mLocalBroadcastManager = null;

    private final IBinder mBinder = new ClientBinder();

    int cloudNum = 1;
    int cloudDanger = 0; // Cd

    private DatabaseHelper db = null;
    private AsyncTask task = null;

    private static DropboxAPI<?> mApi;
    private File mFile;

    private long mFileLen;
    private DropboxAPI.UploadRequest mRequest;
    private Context mContext;

    private String mErrorMsg;

    public VaultClient(DropboxAPI<?> mApi, Context mContext) {
        this.mApi = mApi;
        this.mContext = mContext;
    }

    public VaultClient() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        db = DatabaseHelper.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void setmApi(DropboxAPI<?> mApi) {
        Log.i(TAG, "mApi set in VaultClient");
        this.mApi = mApi;
    }

    private Pair<FECParameters, Integer> getParams(long fileSize) {
        // epsilon
        int overHead = 4;
        int symSize = (int) Math.round(Math.sqrt((float) fileSize * 8
                / (float) overHead)); // symbol header length = 8, T = sqrt(D * delta / epsilon)
        int blockCount = (int) Math.ceil((float)fileSize / (float)30000000);
        FECParameters fecParams = FECParameters.newParameters(fileSize,
                symSize, blockCount);

        int blockSize = (int) Math.ceil((float)fileSize / (float)fecParams.numberOfSourceBlocks());
        int k = (int) Math.ceil((float) blockSize / (float) symSize);

        float gamma = (float) cloudDanger / (float) cloudNum;

        int r = (int) Math.ceil((gamma * k + overHead) / (1 - gamma));

        return Pair.of(fecParams, r);
    }

    public void upload(File file) {
        String uploadPath = "CLOUDVAULT_ANDROID";
        String cloudFilePath;
        String localFileName = file.getName();

        if (uploadPath.length() > 0) {
            cloudFilePath = uploadPath + "/" + localFileName;
        } else {
            cloudFilePath = localFileName;
        }
        long fileSize = file.length();

        ContentValues newFile = new ContentValues(2);
        newFile.put(DatabaseHelper.FILENAME, cloudFilePath);
        newFile.put(DatabaseHelper.SIZE, fileSize);
        task = new InsertTask().execute(newFile);
        uploadFile(file, cloudFilePath);
    }

    //TODO do all this in another thread
    public void uploadFile(File file, String cloudFilePath) {
        // logger.entry();
        try {
            byte[] data = readFileToByteArray(file);
            long fileSize = data.length;
            Log.i(TAG, "Uploading: " + file.getPath());
            Pair<FECParameters, Integer> params = getParams(fileSize);
            FECParameters fecParams = params.first;
            int symSize = fecParams.symbolSize();
            int blockSize = (int) Math.ceil((float)fileSize / (float)fecParams.numberOfSourceBlocks());
            int k = (int) Math.ceil((float) blockSize / (float) symSize);
            int r = params.second;

            ArrayDataEncoder dataEncoder = OpenRQ.newEncoder(data, fecParams);

            int packetID = 0, blockID = 0;
            byte[] packetdata;
            Iterable<SourceBlockEncoder> srcBlkEncoders = dataEncoder
                    .sourceBlockIterable();

            for (SourceBlockEncoder srcBlkEnc : srcBlkEncoders) {
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
                    ByteArrayOutputStream dataArray = dataArrays.get(cloudID);
                    dataArray.write(packetdata, 0, packetdata.length);
                    dataArrays.set(cloudID, dataArray);
                    packetID++;
                }
                //TODO handle upload to all the clouds
                if(mApi==null) {
                    Log.v(TAG, "mApi is null in VaultClient....");
                }
                UploadToDropbox upload = new UploadToDropbox(this, mApi, cloudFilePath + "_" + blockID, dataArrays.get(0));
                upload.execute();
//                cloudsHandler.uploadFile(dataArrays, cloudFilePath + "_"
//                        + blockID, WriteMode.OVERWRITE);

                blockID++;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in uploading File " + cloudFilePath);
        }
    }

    public void download(String cloudFilePath) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class ClientBinder extends Binder {
        VaultClient getService() {
            return VaultClient.this;
        }
    }

    abstract private class BaseTask<T> extends AsyncTask<T, Void, Cursor> {
        @Override
        public void onPostExecute(Cursor result) {
            Intent intent = new Intent(INTENT_FILE_UPLOADED);
            mLocalBroadcastManager.sendBroadcast(intent);
            task=null;
        }

        protected Cursor doQuery() {
            Cursor result=
                    db
                            .getReadableDatabase()
                            .query(DatabaseHelper.TABLE,
                                    new String[] {"ROWID AS _id",
                                            DatabaseHelper.FILENAME,
                                            DatabaseHelper.SIZE},
                                    null, null, null, null, DatabaseHelper.FILENAME);

            result.getCount();

            return(result);
        }
    }

    private class InsertTask extends BaseTask<ContentValues> {
        @Override
        protected Cursor doInBackground(ContentValues... values) {
            db.getWritableDatabase().insert(DatabaseHelper.TABLE,
                    DatabaseHelper.FILENAME, values[0]);

            return(doQuery());
        }
    }
}
