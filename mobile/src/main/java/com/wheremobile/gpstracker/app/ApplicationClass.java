package com.wheremobile.gpstracker.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class ApplicationClass extends MultiDexApplication {

    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        mContext = this;
//        Context context = getApplicationContext();
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context); //temp

//        long synchTime = Long.parseLong(prefs.getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "30"));
//        Intent i = new Intent(context, SynchronizeService.class);
//        i.setAction(SynchronizeService.ACTION_SET_ALARM);
//        i.putExtra(SynchronizeService.EXTRA_TIME, synchTime);
//
//        startWakefulService(context, i);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e("terminate", "onTerminate()");
        try {
            Intent i = getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.wheremobile.gpstracker");
            getApplicationContext().startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
