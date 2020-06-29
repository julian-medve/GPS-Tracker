package com.wheremobile.gpstracker.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.wheremobile.gpstracker.R;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.provider.GPSProvider;
import com.wheremobile.gpstracker.schedular.ServiceScheduler;
import com.wheremobile.gpstracker.service.SynchronizeService;
import com.wheremobile.gpstracker.service.UserLocationService;
import com.wheremobile.gpstracker.utils.AccountUtils;
import com.wheremobile.gpstracker.utils.MyPreference;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";

    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 3L;
    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;

    public static SettingsFragment newInstance() {
        Bundle args = new Bundle();
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setTitle();
    }

    private void setTitle() {
        String title = getString(R.string.title_activity_settings);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        if (null != toolbar) {
            toolbar.setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            default:
                Log.e(TAG, "Wrong element choosen: " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Preference interval = findPreference(Constants.SETTINGS_SYNCHRONIZE_INTERVAL);
        interval.setOnPreferenceChangeListener(this);
        CheckBoxPreference synchOnOff = (CheckBoxPreference) findPreference(Constants.SETTINGS_SYNCHRONIZE);

        synchOnOff.setOnPreferenceChangeListener(this);
        synchOnOff.setDefaultValue(true);

        MyPreference myPreference = MyPreference.getInstance(getActivity());
        boolean isFirstTime = myPreference.getBoolean("isFirstTime");
        if (isFirstTime) {
            Account account = AccountUtils.createSyncAccount(getContext());
            long syncTime;
            myPreference.saveBoolean("isFirstTime", false);
            syncTime = Integer.parseInt(getPreferenceManager().getSharedPreferences().getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
            setAlarmManager(syncTime);
            setSyncAdapter(account, syncTime);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Account account = AccountUtils.createSyncAccount(getContext());
        long syncTime;
        switch (preference.getKey()) {
            case Constants.SETTINGS_SYNCHRONIZE_INTERVAL:
                syncTime = Integer.parseInt((String) newValue);

                cancelAlarmManager();
                setAlarmManager(syncTime);
                setSyncAdapter(account, syncTime);
                return true;
            case Constants.SETTINGS_SYNCHRONIZE:
                try {
                    CheckBoxPreference synchronizePref = ((CheckBoxPreference) preference);
                    boolean switched = synchronizePref.isChecked();
                    synchronizePref.setChecked(!switched);
                    if ((boolean) newValue) { //switched

                        syncTime = Integer.parseInt(getPreferenceManager().getSharedPreferences().getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
                        setAlarmManager(syncTime);
                        setSyncAdapter(account, syncTime);
                    } else {
                        cancelAlarmManager();
                        ContentResolver.removePeriodicSync(account, GPSProvider.AUTHORITY, Bundle.EMPTY);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return false;
        }
    }

    private void setSyncAdapter(Account account, long syncTime) {
        ContentResolver.addPeriodicSync(account, GPSProvider.AUTHORITY, Bundle.EMPTY, syncTime);
    }

    private void setAlarmManager(long syncTime) {

        Intent intent = new Intent(getContext(), SynchronizeService.class);
        intent.setAction(SynchronizeService.ACTION_SET_ALARM);
        intent.putExtra(SynchronizeService.EXTRA_TIME, syncTime);
        getActivity().startService(intent);
    }

    private void cancelAlarmManager() {
        Intent intent = new Intent(getContext(), SynchronizeService.class);
        intent.setAction(SynchronizeService.ACTION_CANCEL_ALARM);
        getActivity().startService(intent);
    }
}
