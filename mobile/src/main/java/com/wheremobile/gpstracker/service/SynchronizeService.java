package com.wheremobile.gpstracker.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.wheremobile.gpstracker.receive.SyncAlarmReceiver;

public class SynchronizeService extends IntentService {

    public static final String ACTION_ALARM_RUN = "action.alarm.run";

    public static final String ACTION_SET_ALARM = "action.alarm.set";
    public static final String ACTION_CANCEL_ALARM = "action.alarm.cancel";

    public static final String EXTRA_TIME = "extra.time";

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public SynchronizeService() {
        this("SynchronizeService");
    }

    public SynchronizeService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent1 = new Intent(this, SyncAlarmReceiver.class);
        intent1.setAction(ACTION_ALARM_RUN);

        pendingIntent = PendingIntent.getBroadcast(this, 1001, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d("logba", "onHandleIntent: ");
        String action = intent.getAction();
        if (ACTION_SET_ALARM.equals(action)) {
            long synchTime = intent.getLongExtra(EXTRA_TIME, 210);
            //long synchTime = 30;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000 * synchTime, pendingIntent);
                Log.e(getClass().getSimpleName(), "onStartCommand: call api");
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 1000 * synchTime, pendingIntent);
            } else {
                Log.d("logba", "onStartCommand: Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT " + (1000 * synchTime));
//                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000 * synchTime, pendingIntent);
                //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (1000 * synchTime), 1000 * synchTime, pendingIntent);

                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 1000 * synchTime, pendingIntent);

//                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000 * synchTime, pendingIntent);
            }
        } else if (ACTION_CANCEL_ALARM.equals(action)) {
            alarmManager.cancel(pendingIntent);
            stopSelf();
        }
    }

}
