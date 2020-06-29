package com.wheremobile.gpstracker.model;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.wheremobile.gpstracker.provider.GPSProvider;

import java.util.Date;

public class GPSModel {

    private long id;
    private String imei;
    private Date date;
    private String timeZone;
    private int batteryLevel;
    private String phone;
    private String charging;

    private double latitude;
    private double longitude;
    private float speed;
    private double altitude;
    private float accuracy;
    private String bearing;
    private String provider;
    private String status;

    public static class GPSContract implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(GPSProvider.CONTENT_URI, "gps");

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/data_gps";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/data_gps";

        public static final String TABLE_NAME = "gps";

        public static final String IMEI = "imei";
        public static final String DATE = "date";
//        public static final String ACTIVITY_DATETIME = "activity_datetime";
//        public static final String ACTIVITY_DATE = "activity_date";
//        public static final String ACTIVITY_TIME = "activity_time";
        public static final String TIME_ZONE = "time_zone";
        public static final String BATTERY_LEVEL = "battery_level";
        public static final String PHONE = "phone";
        public static final String CHARGING = "charging";

        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ACCURACY = "accuracy";
        public static final String SPEED = "speed";
        public static final String ALTITUDE = "altitude";
        public static final String BEARING = "bearing";
        public static final String PROVIDER = "provider";
        public static final String STATUS = "status";


        public static final String[] PROJECTION_ALL = {_ID,
                IMEI,
                DATE,
//                ACTIVITY_DATETIME,
//                ACTIVITY_DATE,
//                ACTIVITY_TIME,
                TIME_ZONE,
                BATTERY_LEVEL,
                PHONE,
                CHARGING,
                LATITUDE,
                LONGITUDE,
                ACCURACY,
                SPEED,
                ALTITUDE,
                BEARING,
                PROVIDER,
                STATUS
        };

        public static final String SORT_ORDER_DEFAULT = DATE + " ASC";
    }

    GPSModel(Builder builder) {
        id = builder.id;
        imei = builder.imei;
        date = builder.date;
        timeZone = builder.timeZone;
        batteryLevel = builder.batteryLevel;
        phone = builder.phone;
        charging = builder.charging;
        latitude = builder.latitude;
        longitude = builder.longitude;
        speed = builder.speed;
        altitude = builder.altitude;
        accuracy = builder.accuracy;
        bearing = builder.bearing;
        provider = builder.provider;
        status = builder.status;
    }

    public long getId() {
        return id;
    }

    public String getImei() {
        return imei;
    }

    public Date getDate() {
        return date;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public String getPhone() {
        return phone;
    }

    public String getCharging() {
        return charging;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public double getAltitude() {
        return altitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getBearing() {
        return bearing;
    }

    public String getProvider() {
        return provider;
    }

    public String getStatus() {
        return status;
    }

    public static class Builder {

        private long id;
        private String imei;
        private Date date;
        private String timeZone;
        private int batteryLevel;
        private String phone;
        private String charging;

        private double latitude;
        private double longitude;
        private float speed;
        private double altitude;
        private float accuracy;
        private String bearing;
        private String provider;
        private String status;

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setImei(String imei) {
            this.imei = imei;
            return this;
        }

        public Builder setDate(Date date) {
            this.date = date;
            return this;
        }

        public Builder setTimeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder setBatteryLevel(int batteryLevel) {
            this.batteryLevel = batteryLevel;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setCharging(String charging) {
            this.charging = charging;
            return this;
        }

        public Builder setLatitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder setLongitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder setSpeed(float speed) {
            this.speed = speed;
            return this;
        }

        public Builder setAltitude(double altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder setAccuracy(float accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public Builder setBearing(String bearing) {
            this.bearing = bearing;
            return this;
        }

        public Builder setProvider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public GPSModel build() {
            return new GPSModel(this);
        }
    }
}
