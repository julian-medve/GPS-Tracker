package com.wheremobile.gpstracker.db.wrapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.wheremobile.gpstracker.model.GPSModel;
import com.wheremobile.gpstracker.model.GPSModel.GPSContract;
import com.wheremobile.gpstracker.utils.CursorUtils;

import java.util.Date;

public class GPSWrapper extends CursorWrapper {

    public GPSWrapper(Cursor cursor) {
        super(cursor);
    }

    public GPSModel getGPSModel() {
        int id = CursorUtils.getInt(GPSContract._ID, this);
        String imei = CursorUtils.getString(GPSContract.IMEI, this);
        Date date = new Date(CursorUtils.getLong(GPSContract.DATE, this));
        String timeZone = CursorUtils.getString(GPSContract.TIME_ZONE, this);
        int batteryLevel = CursorUtils.getInt(GPSContract.BATTERY_LEVEL, this);
        String phone = CursorUtils.getString(GPSContract.PHONE, this);
        String charging = CursorUtils.getString(GPSContract.CHARGING, this);

        double latitude = CursorUtils.getDouble(GPSContract.LATITUDE, this);
        double longitude = CursorUtils.getDouble(GPSContract.LONGITUDE, this);
        float speed = CursorUtils.getFloat(GPSContract.SPEED, this);
        double altitude = CursorUtils.getDouble(GPSContract.ALTITUDE, this);
        float accuracy = CursorUtils.getFloat(GPSContract.ACCURACY, this);
        String bearing = CursorUtils.getString(GPSContract.BEARING, this);
        String provider = CursorUtils.getString(GPSContract.PROVIDER, this);
        String status = CursorUtils.getString(GPSContract.STATUS, this);

        GPSModel.Builder builder = new GPSModel.Builder()
                .setId(id)
                .setImei(imei)
                .setDate(date)
                .setTimeZone(timeZone)
                .setBatteryLevel(batteryLevel)
                .setPhone(phone)
                .setCharging(charging)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setSpeed(speed)
                .setAltitude(altitude)
                .setAccuracy(accuracy)
                .setBearing(bearing)
                .setStatus(status)
                .setProvider(provider);
        return builder.build();
    }
}
