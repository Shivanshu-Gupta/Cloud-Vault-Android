package com.cloudsecurity.cloudvault;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Shivanshu Gupta on 19-Sep-15.
 */
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final String TAG = "CloudVault";
    private static DatabaseHelper db;
    public static final String DATABASE_NAME="vault.db";
    public static String DATABASE_PATH = null;
    private static final int SCHEMA=1;
    static final String FILENAME="cloudFileName";
    static final String SIZE="size";
    static final String CLOUDLIST="cloudList";
    static final String MINCLOUDS="minClouds";
    static final String TIMESTAMP = "timeStamp";
    static final String FILES_TABLE ="fileInfo";

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
        DATABASE_PATH = context.getDatabasePath(DATABASE_NAME).getPath();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if(db ==null) {
            db = new DatabaseHelper(context.getApplicationContext());
        }
        return db;
    }

    public static synchronized DatabaseHelper getInstanceFresh(Context context) {
        db.close();
        db = new DatabaseHelper(context.getApplicationContext());
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT  * FROM " + FILES_TABLE, null);
        Log.v(TAG, "DatabaseHelper : File Count: " + cur.getCount());
        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FILES_TABLE + " (" +
                        FILENAME + " TEXT NOT NULL, " +
                        SIZE + " BIGINT NOT NULL, " +
                        CLOUDLIST + " TEXT NOT NULL, " +
                        MINCLOUDS + " INT NOT NULL, " +
                        TIMESTAMP + " TEXT NOT NULL)"
        );
//        db.execSQL("CREATE TABLE fileSizes (cloudFileName TEXT, size REAL, cloudList TEXT)");
//        ContentValues cv=new ContentValues();
//
//        cv.put(FILENAME, "This");
//        cv.put(SIZE, 4);
//        db.insert(TABLE, FILENAME, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
