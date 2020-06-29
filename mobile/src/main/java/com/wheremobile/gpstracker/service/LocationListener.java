package com.wheremobile.gpstracker.service;


import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class LocationListener implements android.location.LocationListener {

    private static final String TAG = "LocationListener";

    private Context context;
    private Location lastLocation;

    public LocationListener(Context context, String provider) {
        this.context = context;
        lastLocation = new Location(provider);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation.set(location);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "onProviderDisabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "onProviderEnabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(TAG, "onStatusChanged: " + provider);
    }
}
