package com.wheremobile.gpstracker.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.wheremobile.gpstracker.model.GPSModel;

public class DBHelper extends SQLiteOpenHelper {

    private static volatile DBHelper instance;

    private static final String DB_NAME = "gpsdata.db";
    private static final int DB_VERSION = 9;

    public static DBHelper getInstance(Context context) {
        if (null == instance) {
            synchronized (DBHelper.class) {
                if (null == instance) {
                    instance = new DBHelper(context);
                }
            }
        }
        return instance;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.beginTransaction();
        sqLiteDatabase.execSQL(DBSchema.CREATE_TABLE_GPS);
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table " + GPSModel.GPSContract.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
