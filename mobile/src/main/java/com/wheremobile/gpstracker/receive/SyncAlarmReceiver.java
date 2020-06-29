package com.wheremobile.gpstracker.receive;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.wheremobile.gpstracker.schedular.UploadServerDataIntentService;

public class SyncAlarmReceiver extends WakefulBroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("logba", "onReceive: ");
        this.context = context;

        final Intent uploadDataIntent = new Intent(context, UploadServerDataIntentService.class);
        startWakefulService(context, uploadDataIntent);

    }

}