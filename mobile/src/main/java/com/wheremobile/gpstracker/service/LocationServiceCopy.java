package com.wheremobile.gpstracker.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.wheremobile.gpstracker.R;
import com.wheremobile.gpstracker.activity.SplashScreen;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.model.GPSModel.GPSContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class LocationServiceCopy extends Service implements Handler.Callback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "UserLocationService";

    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final float LOCATION_DISTANCE = 0.100f;

    private TelephonyManager telephonyManager = null;
    private LocationManager locationManager = null;
    private NotificationManager notificationManager = null;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private GoogleApiClient mApiClient;

    private int batteryStatus = 1;
    private int batteryLevel = -1;

    private Handler handler;
    private LocationUpdateTask locationUpdateTask;
    private static Timer timer = new Timer();

    private static Location currentLocation;

    public static final String MOVING = "Moving";
    public static final String UNKNOWN = "";
    public static final String IDLE = "Idle";
    public static final String ACTION = "android.intent.action.MOVEMENT_CHANGED";
    private String deviceStatus = IDLE;
    private PendingIntent pendingIntent;

    LocationListener[] locationListeners = new LocationListener[]{
            new LocationListener(this, LocationManager.GPS_PROVIDER),
            new LocationListener(this, LocationManager.NETWORK_PROVIDER)
    };

    SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = (preference, key) -> {
        switch (key) {
            case Constants.SETTINGS_UPDATE_INTERVAL:
                int seconds = Integer.valueOf(preference.getString(Constants.SETTINGS_UPDATE_INTERVAL, "60"));
                int timeInterval = MILLISECONDS_IN_SECOND * seconds;
                startService(timeInterval);
                break;
        }
    };

    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            parseBatteryIntent(intent);
        }
    };

    BroadcastReceiver deviceMovementReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final ArrayList<DetectedActivity> detectedActivities = intent.getParcelableArrayListExtra("data");
            for (DetectedActivity activity : detectedActivities) {
                Log.e("TAG", "handleDetectedActivities: " + activity.getType());
                switch (activity.getType()) {
                    case DetectedActivity.STILL:
                        //idle
                        deviceStatus = LocationServiceCopy.IDLE;
                        break;
                    case DetectedActivity.UNKNOWN:
                        //unknown
                        deviceStatus = LocationServiceCopy.UNKNOWN;
                        break;
                    default:
                        //moving
                        deviceStatus = LocationServiceCopy.MOVING;
                        break;
                }
            }
        }
    };

    private void startService(int timeInterval) {
        if (null != locationUpdateTask) {
            locationUpdateTask.cancel();
            timer.purge();
        }
        locationUpdateTask = new LocationUpdateTask();
        timer.scheduleAtFixedRate(locationUpdateTask, 0, timeInterval);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public boolean handleMessage(Message msg) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("logba", "handleMessage: check GPS_PROVIDER 1");
            Location gotLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (null == gotLoc) {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    Log.d("logba", "handleMessage: NETWORK_PROVIDER 1");
                    gotLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
            gotLocation(gotLoc);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.d("logba", "handleMessage: NETWORK_PROVIDER 2");
            Location gotLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (null == gotLoc) {
                Log.d("logba", "handleMessage: check GPS_PROVIDER 2");
                gotLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            gotLocation(gotLoc);
        }
        return false;
    }

    private void gotLocation(Location location) {
        Log.d("logba", "UserLocationService -> gotLocation : " + location);
        currentLocation = location;
        Intent i = new Intent(Constants.ACTION_SERVICE_DATA);
        i.putExtra(Constants.EXTRA_LOCATION, currentLocation);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        storeGPS(currentLocation);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        final Intent intent = new Intent(this, ActivityRecognizedService.class);
        pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 0, pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class LocationUpdateTask extends TimerTask {
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent serviceIntent, int flags, int startId) {
        super.onStartCommand(serviceIntent,flags,startId);
        Log.d("logba", "onCreate: MAAAAAAAAAAAIN");

        initializePowerManager();
        initializeLocationManager();
        initializeTelephoneManager();
        initializeNotificationManager();
        initializeGoogleApiClient();

        final Intent inten = new Intent(this, SplashScreen.class);
        inten.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent activityIntent = PendingIntent.getActivity(this, 1122, inten, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(activityIntent)
                .setSmallIcon(R.mipmap.ic_app_icon)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Now working Location service")
                .build();

        startForeground(7788, notification);

        addListeners();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);

        int seconds = Integer.valueOf(prefs.getString(Constants.SETTINGS_UPDATE_INTERVAL, "60"));
        //int seconds = Integer.valueOf(prefs.getString(Constants.SETTINGS_UPDATE_INTERVAL, "10"));
        int intervalTime = MILLISECONDS_IN_SECOND * seconds;
        handler = new Handler(this);
        startService(intervalTime);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        registerReceiver(deviceMovementReceiver, new IntentFilter(ACTION));
        Intent intent = registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        parseBatteryIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        /*Log.d("logba", "onCreate: MAAAAAAAAAAAIN");

        initializePowerManager();
        initializeLocationManager();
        initializeTelephoneManager();
        initializeNotificationManager();
        initializeGoogleApiClient();

        final Intent inten = new Intent(this, SplashScreen.class);
        inten.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent activityIntent = PendingIntent.getActivity(this, 1122, inten, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(activityIntent)
                .setSmallIcon(R.mipmap.ic_app_icon)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Now working Location service")
                .build();

        startForeground(7788, notification);

        addListeners();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);

        int seconds = Integer.valueOf(prefs.getString(Constants.SETTINGS_UPDATE_INTERVAL, "60"));
        //int seconds = Integer.valueOf(prefs.getString(Constants.SETTINGS_UPDATE_INTERVAL, "10"));
        int intervalTime = MILLISECONDS_IN_SECOND * seconds;
        handler = new Handler(this);
        startService(intervalTime);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        registerReceiver(deviceMovementReceiver, new IntentFilter(ACTION_DEVICE_MOVEMENT));
        Intent intent = registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        parseBatteryIntent(intent);*/
    }

    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

        mApiClient.connect();
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        releaseListeners();
        releaseGoogleApiClient();

        unregisterReceiver(batteryLevelReceiver);
        unregisterReceiver(deviceMovementReceiver);

        Log.e(TAG, "onDestroy: ");
        super.onDestroy();

        PendingIntent service = PendingIntent.getService(
                getApplicationContext(),
                1251,
                new Intent(getApplicationContext(), LocationServiceCopy.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, 5000, service);
    }

    private void releaseGoogleApiClient() {
        if (mApiClient != null && mApiClient.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, pendingIntent);
            mApiClient.disconnect();
        }
    }

    private void initializeLocationManager() {
        if (null == locationManager) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void addListeners() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, LOCATION_DISTANCE, locationListeners[0]);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, LOCATION_DISTANCE, locationListeners[1]);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void releaseListeners() {
        if (null != locationManager) {
            for (LocationListener locationListener : locationListeners) {
                try {
                    locationManager.removeUpdates(locationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    private void initializeNotificationManager() {
        if (null == notificationManager) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

    private void initializePowerManager() {
        if (null == powerManager) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
    }

    private void initializeTelephoneManager() {
        if (null == telephonyManager) {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    private void releaseWakeLock() {
        if (null != wakeLock) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void parseBatteryIntent(Intent intent) {
        batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (rawlevel >= 0 && scale > 0) {
            batteryLevel = (rawlevel * 100) / scale;
        }
    }

    @SuppressLint("MissingPermission")
    private Uri storeGPS(Location location) {
        if (null == location) return null;

        final ContentResolver contentResolver = getContentResolver();

        final ContentValues gpsValues = new ContentValues();
        gpsValues.put(GPSContract.IMEI, telephonyManager.getDeviceId());
        gpsValues.put(GPSContract.DATE, System.currentTimeMillis());
        gpsValues.put(GPSContract.TIME_ZONE, getTimeZone(location));
        gpsValues.put(GPSContract.BATTERY_LEVEL, batteryLevel);
        //gpsValues.put(GPSContract.PHONE, telephonyManager.getLine1Number());
        gpsValues.put(GPSContract.PHONE, "");

        gpsValues.put(GPSContract.CHARGING, parseBatteryStatus(batteryStatus));

        final Uri uri = GPSContract.CONTENT_URI;
        final String[] projection = null;
        final String selection = null;
        final String[] selectionArgs = null;
        final String sortOrder = null;
        final Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToLast();
            final String status = cursor.getString(cursor.getColumnIndex(GPSContract.STATUS));
            if (!TextUtils.isEmpty(status) && status.equalsIgnoreCase(IDLE) && deviceStatus.equalsIgnoreCase(status)) {

                gpsValues.put(GPSContract.LATITUDE, cursor.getDouble(cursor.getColumnIndex(GPSContract.LATITUDE)));
                gpsValues.put(GPSContract.LONGITUDE, cursor.getDouble(cursor.getColumnIndex(GPSContract.LONGITUDE)));
                gpsValues.put(GPSContract.PROVIDER, cursor.getString(cursor.getColumnIndex(GPSContract.PROVIDER)));

                gpsValues.put(GPSContract.SPEED, cursor.getFloat(cursor.getColumnIndex(GPSContract.SPEED)));
                gpsValues.put(GPSContract.ALTITUDE, cursor.getDouble(cursor.getColumnIndex(GPSContract.ALTITUDE)));
                gpsValues.put(GPSContract.BEARING, cursor.getString(cursor.getColumnIndex(GPSContract.BEARING)));
                gpsValues.put(GPSContract.ACCURACY, cursor.getFloat(cursor.getColumnIndex(GPSContract.ACCURACY)));
                Log.e(TAG, "storeGPS from db: " + status);
                gpsValues.put(GPSContract.STATUS, status);

            } else {
                gpsValues.put(GPSContract.LATITUDE, (double) location.getLatitude());
                gpsValues.put(GPSContract.LONGITUDE, (double) location.getLongitude());
                gpsValues.put(GPSContract.PROVIDER, location.getProvider());

                if (location.hasSpeed()) {
                    gpsValues.put(GPSContract.SPEED, (float) location.getSpeed());
                }
                if (location.hasAltitude()) {
                    gpsValues.put(GPSContract.ALTITUDE, (double) location.getAltitude());
                }
                if (location.hasBearing()) {
                    gpsValues.put(GPSContract.BEARING, parseDegree(location.getBearing()));
                }
                if (location.hasAccuracy()) {
                    gpsValues.put(GPSContract.ACCURACY, (float) location.getAccuracy());
                }
                Log.e(TAG, "storeGPS: " + deviceStatus);
                gpsValues.put(GPSContract.STATUS, deviceStatus);
            }
            cursor.close();
        } else {
            gpsValues.put(GPSContract.LATITUDE, (double) location.getLatitude());
            gpsValues.put(GPSContract.LONGITUDE, (double) location.getLongitude());
            gpsValues.put(GPSContract.PROVIDER, location.getProvider());

            if (location.hasSpeed()) {
                gpsValues.put(GPSContract.SPEED, (float) location.getSpeed());
            }
            if (location.hasAltitude()) {
                gpsValues.put(GPSContract.ALTITUDE, (double) location.getAltitude());
            }
            if (location.hasBearing()) {
                gpsValues.put(GPSContract.BEARING, parseDegree(location.getBearing()));
            }
            if (location.hasAccuracy()) {
                gpsValues.put(GPSContract.ACCURACY, (float) location.getAccuracy());
            }
            Log.e(TAG, "storeGPS: " + deviceStatus);
            gpsValues.put(GPSContract.STATUS, deviceStatus);
        }
        return contentResolver.insert(GPSContract.CONTENT_URI, gpsValues);
    }

    @Nullable
    private String parseDegree(float degree) {
        if (degree == 0 && degree < 45 || degree >= 315 && degree == 360) {
            return "Northbound";
        }
        if (degree >= 45 && degree < 90) {
            return "NorthEastbound";
        }
        if (degree >= 90 && degree < 135) {
            return "Eastbound";
        }
        if (degree >= 135 && degree < 180) {
            return "SouthEastbound";
        }
        if (degree >= 180 && degree < 225) {
            return "SouthWestbound";
        }
        if (degree >= 225 && degree < 270) {
            return "Westbound";
        }
        if (degree >= 270 && degree < 315) {
            return "NorthWestbound";
        }
        return null;
    }

    private String parseBatteryStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                return "Unknown";
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "Not charging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Full";
            default:
                throw new IllegalArgumentException("Unknown status : " + status);
        }
    }

    private String getTimeZone(@NonNull Location location) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(location.getTime());
        TimeZone timeZone = calendar.getTimeZone();
        return timeZone.getID();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        onDestroy();
    }
}
