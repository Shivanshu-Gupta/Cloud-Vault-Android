package com.cloudsecurity.cloudvault;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by Shivanshu Gupta on 19-Sep-15.
 */
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final String TAG = "CloudVault";
    private static DatabaseHelper db;
    public static final String DATABASE_NAME="vault.db";
    private static final int SCHEMA=1;
    static final String FILENAME="cloudFileName";
    static final String SIZE="size";
    static final String CLOUDLIST="cloudList";
    static final String TIMESTAMP = "timeStamp";
    static final String TABLE="fileInfo";

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if(db==null) {
            db = new DatabaseHelper(context.getApplicationContext());
        }
        return db;
    }

    public static synchronized DatabaseHelper getInstanceFresh(Context context) {
        db = new DatabaseHelper(context.getApplicationContext());
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT  * FROM " + TABLE, null);
        Log.v(TAG, "DatabaseHelper : File Count: " + cur.getCount());
        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE fileInfo (" +
                        FILENAME + " TEXT," +
                        SIZE + " REAL," +
                        CLOUDLIST + " TEXT," +
                        TIMESTAMP + " TEXT)"
        );
//        db.execSQL("CREATE TABLE fileSizes (cloudFileName TEXT, size REAL, cloudList TEXT)");
//        ContentValues cv=new ContentValues();
//
//        cv.put(FILENAME, "This");
//        cv.put(SIZE, 4);
//        db.insert(TABLE, FILENAME, cv);
//
//        cv.put(FILENAME, "is");
//        cv.put(SIZE, 2);
//        db.insert(TABLE, FILENAME, cv);
//
//        cv.put(FILENAME, "temporary");
//        cv.put(SIZE, 9);
//        db.insert(TABLE, FILENAME, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
