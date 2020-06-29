package com.wheremobile.gpstracker.service;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.wheremobile.gpstracker.utils.AccountUtils.createSyncAccount;

public class RestartServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("RestartServiceReceiver", "onReceive: ");

        Account account = createSyncAccount(context);
        Intent locationIntent = new Intent(context, UserLocationService.class);
        locationIntent.putExtra("extra.account", account);
        context.stopService(intent);
        context.startService(locationIntent);

    }
}
