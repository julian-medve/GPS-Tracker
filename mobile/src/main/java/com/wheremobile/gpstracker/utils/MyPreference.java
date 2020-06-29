package com.wheremobile.gpstracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by theone on 26/05/17.
 */


public class MyPreference {
    private static MyPreference yourPreference;
    private SharedPreferences sharedPreferences;

    public static MyPreference getInstance(Context context) {
        if (yourPreference == null) {
            yourPreference = new MyPreference(context);
        }
        return yourPreference;
    }

    private MyPreference(Context context) {
        sharedPreferences = context.getSharedPreferences("com.wheremobile.gpsTracker", Context.MODE_PRIVATE);
    }

    public void saveData(String key, String value) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(key, value);
        prefsEditor.commit();
    }

    public String getData(String key) {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, "");
        }
        return "";
    }

    public void saveBoolean(String key, Boolean flag) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putBoolean(key, flag);
        prefsEditor.commit();
    }

    public Boolean getBoolean(String key) {
        if (sharedPreferences != null) {
            return sharedPreferences.getBoolean(key, true);
        }
        return false;
    }
}
