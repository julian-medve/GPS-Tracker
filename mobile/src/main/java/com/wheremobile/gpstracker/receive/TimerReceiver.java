package com.wheremobile.gpstracker.receive;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.wheremobile.gpstracker.schedular.UploadServerDataIntentService;

import static com.wheremobile.gpstracker.service.UserLocationService.ACTION_LOCATION_UPDATE;

public class TimerReceiver extends WakefulBroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("logba", "onReceive: ");
        this.context = context;
        Intent intent1 = new Intent(ACTION_LOCATION_UPDATE);
        context.sendBroadcast(intent1);
    }
}