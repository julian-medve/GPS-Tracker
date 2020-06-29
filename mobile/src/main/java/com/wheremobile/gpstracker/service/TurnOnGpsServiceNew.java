package com.wheremobile.gpstracker.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by theone on 25/05/17.
 */

public class TurnOnGpsServiceNew extends Service {
    public TurnOnGpsServiceNew() {
    }

    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("toTurnOnGps", true)) {
            Log.d("TurnOnGpsService", "canToggleGps " + this.canToggleGPS());
            this.turnOnGPS();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void turnOnGPS() {
        Log.d("TurnOnGpsService", "set GPS on");
        String providers = Settings.Secure.getString(this.getContentResolver(), "location_providers_allowed");
        Intent e;
        if (!providers.contains("gps")) {
            Log.d("TurnOnGpsService", "true");
            e = new Intent();
            e.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            e.addCategory("android.intent.category.ALTERNATIVE");
            e.setData(Uri.parse("3"));
            this.sendBroadcast(e);
        } else {
            Log.d("TurnOnGpsService", "false");
        }

        try {
            Settings.Secure.putString(this.getContentResolver(), "location_providers_allowed", String.format("%s,%s", new Object[]{Settings.Secure.getString(this.getContentResolver(), "location_providers_allowed"), "gps"}));
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 3);
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        try {
            e = new Intent("android.location.GPS_ENABLED_CHANGE");
            e.putExtra("enabled", true);
            this.sendBroadcast(e);
            Log.d("TurnOnGpsService", "send broadcast");
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    private boolean canToggleGPS() {
        PackageManager pacman = this.getPackageManager();
        PackageInfo pacInfo = null;

        try {
            pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        } catch (PackageManager.NameNotFoundException var7) {
            return false;
        }

        if (pacInfo != null) {
            Log.d("TurnOnGpsService", "pacInfo not null");
            try {
                ActivityInfo[] e = pacInfo.receivers;
                int var4 = e.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    ActivityInfo actInfo = e[var5];
                    if (actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported) {
                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d("TurnOnGpsService", "pacInfo is null");
        }

        return false;
    }
}
