package com.wheremobile.gpstracker.db;

import static com.wheremobile.gpstracker.model.GPSModel.GPSContract;

final class DBSchema {

    static final String CREATE_TABLE_GPS = "CREATE TABLE " + GPSContract.TABLE_NAME + " (" +
            GPSContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            GPSContract.IMEI + " TEXT, " +
            GPSContract.DATE + " INTEGER NOT NULL, " +
//            GPSContract.ACTIVITY_DATE + " INTEGER NOT NULL, " +
//            GPSContract.ACTIVITY_TIME + " INTEGER NOT NULL, " +
//            GPSContract.ACTIVITY_DATETIME + " INTEGER NOT NULL, " +
            GPSContract.TIME_ZONE + " TEXT, " +
            GPSContract.BATTERY_LEVEL + " INTEGER NOT NULL, " +
            GPSContract.PHONE + " TEXT, " +
            GPSContract.CHARGING + " TEXT NOT NULL," +
            GPSContract.LATITUDE + " REAL NOT NULL," +
            GPSContract.LONGITUDE + " REAL NOT NULL," +
            GPSContract.ACCURACY + " REAL, " +
            GPSContract.SPEED + " REAL, " +
            GPSContract.ALTITUDE + " REAL, " +
            GPSContract.BEARING + " TEXT, " +
            GPSContract.PROVIDER + " TEXT, " +
            GPSContract.STATUS + " TEXT);";
}
