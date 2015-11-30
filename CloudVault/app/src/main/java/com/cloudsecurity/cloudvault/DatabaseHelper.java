package com.cloudsecurity.cloudvault;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Shivanshu Gupta on 19-Sep-15.
 */
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final String TAG = "CloudVault";
    private static Context context;
    private static DatabaseHelper dbh;
    public static final String DATABASE_NAME="vault.db";
    public static String DATABASE_PATH = null;
    private static final int SCHEMA=1;
    static final String FILENAME="cloudFileName";
    static final String SIZE="size";
    static final String CLOUDLIST="cloudList";
    static final String TIMESTAMP = "timeStamp";
    static final String TABLE="fileInfo";
    SQLiteDatabase db;
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
        DATABASE_PATH = context.getDatabasePath(DATABASE_NAME).getPath();
        this.context = context.getApplicationContext();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if(dbh ==null) {
            dbh = new DatabaseHelper(context.getApplicationContext());
        }
        return dbh;
    }

    public static synchronized DatabaseHelper getInstanceFresh(Context context) {
        dbh.close();
//        copyDatabase();
        dbh = new DatabaseHelper(context.getApplicationContext());
        Cursor cur = dbh.getReadableDatabase().rawQuery("SELECT  * FROM " + TABLE, null);
        Log.v(TAG, "DatabaseHelper : File Count: " + cur.getCount());
        return dbh;
    }

//    @Override
//    public synchronized SQLiteDatabase getWritableDatabase() {
//        try {
//            if (db != null) {
//                if (db.isOpen()) {
//                    return db;
//                }
//            }
//            return SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Override
//    public synchronized SQLiteDatabase getReadableDatabase() {
//        try {
//            if (db != null) {
//                if (db.isOpen()) {
//                    return db;
//                }
//            }
//            return SQLiteDatabase.openOrCreateDatabase(DATABASE_PATH, null, null);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Override
//    public synchronized void close() {
//        if (db != null) {
//            db.close();
//            db = null;
//        }
//        super.close();
//    }

    private static void copyDatabase() {
        // by calling this line an empty database will be created into the default system path
        // of this app - we will then overwrite this with the database from the server
        OutputStream os = null;
        InputStream is = null;
        try {
            // Log.d(TAG, "Copying DB from server version into app");
            is = new FileInputStream(context.getApplicationInfo().dataDir + "/databases/temp.db");
            os = new FileOutputStream(context.getApplicationInfo().dataDir + "/databases/" + DATABASE_NAME);

            copyFile(os, is);
        } catch (Exception e) {
            Log.e(TAG, "Server Database was not found - did it download correctly?", e);
        } finally {
            try {
                //Close the streams
                if(os != null){
                    os.close();
                }
                if(is != null){
                    is.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "failed to close databases");
            }
        }
        // Log.d(TAG, "Done Copying DB from server");
    }




    private static void copyFile(OutputStream os, InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while((length = is.read(buffer))>0){
            os.write(buffer, 0, length);
        }
        os.flush();
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE fileInfo (" +
                        FILENAME + " TEXT," +
                        SIZE + " REAL," +
                        CLOUDLIST + " TEXT," +
                        TIMESTAMP + " TEXT)"
        );
//        dbh.execSQL("CREATE TABLE fileSizes (cloudFileName TEXT, size REAL, cloudList TEXT)");
//        ContentValues cv=new ContentValues();
//
//        cv.put(FILENAME, "This");
//        cv.put(SIZE, 4);
//        dbh.insert(TABLE, FILENAME, cv);
//
//        cv.put(FILENAME, "is");
//        cv.put(SIZE, 2);
//        dbh.insert(TABLE, FILENAME, cv);
//
//        cv.put(FILENAME, "temporary");
//        cv.put(SIZE, 9);
//        dbh.insert(TABLE, FILENAME, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
