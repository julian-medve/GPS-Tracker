package com.wheremobile.gpstracker.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import com.wheremobile.gpstracker.service.TurnOnGpsServiceNew;

/**
 * Created by theone on 25/05/17.
 */

public class GpsStateBroadcastReceiverNew extends BroadcastReceiver {
    public GpsStateBroadcastReceiverNew() {
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            Log.d("GpsBR", "gps state changed");
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled("gps");
            Log.d("GpsBr", "isGpsEnabled " + isGpsEnabled);
            if (!isGpsEnabled) {
                Intent serviceIntent = new Intent(context, TurnOnGpsServiceNew.class);
                serviceIntent.putExtra("toTurnOnGps", true);
                context.startService(serviceIntent);
            }
        }
    }
}