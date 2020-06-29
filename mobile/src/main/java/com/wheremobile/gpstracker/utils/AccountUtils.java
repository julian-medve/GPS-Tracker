package com.wheremobile.gpstracker.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.provider.GPSProvider;

import static com.wheremobile.gpstracker.fragment.SettingsFragment.SYNC_INTERVAL;

public class AccountUtils {

    public static Account createSyncAccount(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        if(deviceId == null || "".equals(deviceId)){
            deviceId = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        Account account = new Account(deviceId, Constants.ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, GPSProvider.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, GPSProvider.AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, GPSProvider.AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);
        }
        return account;
    }
}
