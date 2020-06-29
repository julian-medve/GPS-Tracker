package com.wheremobile.gpstracker.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.letmecode.alivegpsproject.MainActivity;
import com.wheremobile.gpstracker.R;
import com.wheremobile.gpstracker.activity.SplashScreen;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.model.GPSModel.GPSContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class UserLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
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

    private LocationUpdateTask locationUpdateTask;
    private static Timer timer = new Timer();

    private static Location currentLocation;

    public static final String MOVING = "Moving";
    public static final String UNKNOWN = "";
    public static final String IDLE = "Idle";
    public static final String ACTION_DEVICE_MOVEMENT = "android.intent.action.MOVEMENT_CHANGED";
    public static final String ACTION_SERVICE_STOPPED = "android.intent.action.SERVICE_STOPPED";
    public static final String ACTION_LOCATION_UPDATE = "android.intent.action.LOCATION_UPDATE";
    private String deviceStatus = IDLE;
    private PendingIntent pendingIntent;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private HandlerThread thread;
    public static boolean isRepeated = false;
    private AlarmManager alarmManager;


    private LocationListener[] locationListeners = new LocationListener[]{
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

    private BroadcastReceiver deviceMovementReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final ArrayList<DetectedActivity> detectedActivities = intent.getParcelableArrayListExtra("data");
            for (DetectedActivity activity : detectedActivities) {
                Log.e("TAG", "handleDetectedActivities: " + activity.getType());
                switch (activity.getType()) {
                    case DetectedActivity.STILL:
                        //idle
                        deviceStatus = UserLocationService.IDLE;
                        Log.i(TAG, "deviceMovementReceiver: " + deviceStatus);
                        break;
                    case DetectedActivity.UNKNOWN:
                        //unknown
                        deviceStatus = UserLocationService.UNKNOWN;
                        Log.i(TAG, "deviceMovementReceiver: " + deviceStatus);
                        break;
                    default:
                        //moving
                        deviceStatus = UserLocationService.MOVING;
                        Log.i(TAG, "deviceMovementReceiver: " + deviceStatus);
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
        //alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //Intent intent1 = new Intent(this, TimerReceiver.class);
        //pendingIntent = PendingIntent.getBroadcast(this, 1008, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), timeInterval, pendingIntent);
    }


    @SuppressWarnings("MissingPermission")
    public boolean gotMessage() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("logba", "handleMessage: check GPS_PROVIDER 1");
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MILLISECONDS_IN_SECOND,
                    LOCATION_DISTANCE, locationListeners[0]);
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
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MILLISECONDS_IN_SECOND,
                    LOCATION_DISTANCE, locationListeners[1]);
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
        try {
            final Intent intent = new Intent(this, ActivityRecognizedService.class);
            pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 0, pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class LocationUpdateTask extends TimerTask {
        public void run() {
            //handler.sendEmptyMessage(0);
            //mServiceHandler.sendEmptyMessage(0);
            final Intent intent = new Intent(ACTION_LOCATION_UPDATE);
            sendBroadcast(intent);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent serviceIntent, int flags, int startId) {
        super.onStartCommand(serviceIntent, flags, startId);
        Log.d("logba", "onStartCommand: MAAAAAAAAAAAIN");

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

        String channelId = "channel";
        String channelName = "Channel Name";

        NotificationManager notifManager

                = (NotificationManager) getSystemService  (Context.NOTIFICATION_SERVICE);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);

            notifManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), channelId);

        Intent notificationIntent = new Intent(getApplicationContext()
                , MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestID = (int) System.currentTimeMillis();

        PendingIntent pendingIntent
                = PendingIntent.getActivity(getApplicationContext()
                , requestID
                , notificationIntent
                , PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentTitle(getString(R.string.app_name)) // app name
                .setDefaults(Notification.DEFAULT_ALL) // alarm , sound
                .setContentText("Now working Location service")
                .setSmallIcon(R.mipmap.ic_app_icon)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(activityIntent)
                .setLargeIcon(BitmapFactory.decodeResource(getResources()
                        , R.mipmap.ic_app_icon))
        //        .setBadgeIconType(R.mipmap.ic_app_icon)
        ;

        //notifManager.notify(0, builder.build());

        addListeners();
        startForeground(11, builder.getNotification());

        thread = new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        // start the service using the background handler
        mServiceHandler = new ServiceHandler(mServiceLooper);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);

        int seconds = Integer.valueOf(prefs.getString(Constants.SETTINGS_UPDATE_INTERVAL, "60"));
        int intervalTime = MILLISECONDS_IN_SECOND * seconds;
        startService(intervalTime);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        registerReceiver(wakeFullReceiver, new IntentFilter(ACTION_LOCATION_UPDATE));
        registerReceiver(deviceMovementReceiver, new IntentFilter(ACTION_DEVICE_MOVEMENT));

        Intent intent = registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        parseBatteryIntent(intent);
        return START_STICKY;
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
        resetProperties();

        Log.e(TAG, "onDestroy: ");
        super.onDestroy();

        /*final Intent intent = new Intent(ACTION_SERVICE_STOPPED);
        sendBroadcast(intent);*/
    }

    private void resetProperties() {
        releaseWakeLock();
        releaseListeners();
        releaseGoogleApiClient();

        unregisterReceiver(batteryLevelReceiver);
        unregisterReceiver(deviceMovementReceiver);
        unregisterReceiver(wakeFullReceiver);

        if (thread != null && !thread.isInterrupted()) {
            thread.isInterrupted();
        }
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

    private Uri storeGPS(Location location) {
        if (null == location) return null;

        final ContentResolver contentResolver = getContentResolver();

        final ContentValues gpsValues = new ContentValues();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        gpsValues.put(GPSContract.IMEI, telephonyManager.getDeviceId());
        gpsValues.put(GPSContract.DATE, System.currentTimeMillis());
        gpsValues.put(GPSContract.TIME_ZONE, getTimeZone(location));
        gpsValues.put(GPSContract.BATTERY_LEVEL, batteryLevel);
        //gpsValues.put(GPSContract.PHONE, telephonyManager.getLine1Number());
        gpsValues.put(GPSContract.PHONE, "");
        gpsValues.put(GPSContract.CHARGING, parseBatteryStatus(batteryStatus));

        /*gpsValues.put(GPSContract.LATITUDE, (double) location.getLatitude());
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
        Log.e(TAG, "storeGPS: ");
        gpsValues.put(GPSContract.STATUS, "");*/

        final Uri uri = GPSContract.CONTENT_URI;
        final String[] projection = null;
        final String selection = null;
        final String[] selectionArgs = null;
        final String sortOrder = null;
        final Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToLast();
            final String status = cursor.getString(cursor.getColumnIndex(GPSContract.STATUS));
            Log.d("System out", "cursor status in set data in content uri " + status);
           /* if (!TextUtils.isEmpty(status) && status.equalsIgnoreCase(IDLE) && deviceStatus.equalsIgnoreCase(status)) {


                gpsValues.put(GPSContract.LATITUDE, cursor.getDouble(cursor.getColumnIndex(GPSContract.LATITUDE)));
                gpsValues.put(GPSContract.LONGITUDE, cursor.getDouble(cursor.getColumnIndex(GPSContract.LONGITUDE)));
                gpsValues.put(GPSContract.PROVIDER, cursor.getString(cursor.getColumnIndex(GPSContract.PROVIDER)));

                gpsValues.put(GPSContract.SPEED, cursor.getFloat(cursor.getColumnIndex(GPSContract.SPEED)));
                gpsValues.put(GPSContract.ALTITUDE, cursor.getDouble(cursor.getColumnIndex(GPSContract.ALTITUDE)));
                gpsValues.put(GPSContract.BEARING, cursor.getString(cursor.getColumnIndex(GPSContract.BEARING)));
                gpsValues.put(GPSContract.ACCURACY, cursor.getFloat(cursor.getColumnIndex(GPSContract.ACCURACY)));
                Log.e(TAG, "storeGPS from db: " + status);
                gpsValues.put(GPSContract.STATUS, status);

            } else {*/
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

            // }
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
        /*resetProperties();*/
        super.onTaskRemoved(rootIntent);
        Log.e(TAG, "onTaskRemoved: ");

        final Intent intent = new Intent(ACTION_SERVICE_STOPPED);
        sendBroadcast(intent);
    }

    // Object responsible for
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            gotMessage();
        }
    }

    private BroadcastReceiver wakeFullReceiver = new WakefulBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            gotMessage();
            completeWakefulIntent(intent);
        }

    };
}