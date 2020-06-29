package com.wheremobile.gpstracker.receive;
import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.wheremobile.gpstracker.activity.SplashScreen;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.service.UserLocationService;
import com.wheremobile.gpstracker.service.SynchronizeService;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;
import static com.wheremobile.gpstracker.utils.AccountUtils.createSyncAccount;
public class BootCompletedReceiver extends BroadcastReceiver {

    private static Intent splashIntent;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.i("cccccc", "deviceMovementReceiver: " + "vvvvvvvvvvvvvvvv");
            Account account = createSyncAccount(context);
            Intent locationIntent = new Intent(context, UserLocationService.class);
            locationIntent.putExtra("extra.account", account);
            context.startService(locationIntent);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            long synchTime = Long.parseLong(prefs.getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
            Intent i = new Intent(context, SynchronizeService.class);
            i.setAction(SynchronizeService.ACTION_SET_ALARM);
            i.putExtra(SynchronizeService.EXTRA_TIME, synchTime);
            startWakefulService(context, i);

            splashIntent = new Intent(context, SplashScreen.class);
            splashIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            splashIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(splashIntent);
        }
    }
}
