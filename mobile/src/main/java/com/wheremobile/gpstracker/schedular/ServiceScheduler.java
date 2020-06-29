package com.wheremobile.gpstracker.schedular;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.utils.Const;

public final class ServiceScheduler {

    private static final String MESSENGER_ENABLED = "com.wheremobile.gpstracker.networkmanager.MESSENGER_ENABLED";

    private static final long MINUTES_IN_SECONDS = 60;
    private static final long HOURS_IN_MINUTES = 60;

    private boolean isEnabled;
    private Context context;
    private static ServiceScheduler instance;
    private boolean isPeriodic = false;

    public static final String TAG_TASK_ONE_OFF_LOG = "TAG_TASK_ONE_OFF_LOG";
    public static final String TAG_TASK_PERIODIC_LOG = "TAG_TASK_PERIODIC_LOG";

    private FirebaseJobDispatcher dispatcher;

    public static ServiceScheduler newInstance(Context context) {

        Context safeContext = context.getApplicationContext();
        boolean isEnabled = Const.getBoolean(context, MESSENGER_ENABLED, false);
        return new ServiceScheduler(context, isEnabled);
    }

    private ServiceScheduler(Context context, boolean isEnabled) {
        instance = this;
        this.isEnabled = isEnabled;
        this.context = context;
        dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this.context));
    }

    public void startService() {
        isEnabled = true;
        saveEnabledState();
        scheduleOneOff();
    }

    public void startService(boolean isPeriodic) {
        isEnabled = true;
        this.isPeriodic = isPeriodic;
        saveEnabledState();
        if (isPeriodic) {
            scheduleRepeat();
        } else
            scheduleOneOff();
    }

    public void stopService() {
        cancelAll();
        isEnabled = false;
        saveEnabledState();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    private void saveEnabledState() {
        Const.storeBoolean(context, MESSENGER_ENABLED, isEnabled);
    }

    public void scheduleOneOff() {

        final Job locationJob = dispatcher.newJobBuilder()
                .setService(LocationSchedulerService.class)
                .setTag(TAG_TASK_ONE_OFF_LOG)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(29, 31))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        dispatcher.mustSchedule(locationJob);
        Log.v(getClass().getSimpleName(), "oneoff task scheduled");

        /*try {
            PeriodicTask periodic = new PeriodicTask.Builder()
                    .setService(NetworkService.class)
                    .setPeriod(10)
                    .setTag(TAG_TASK_ONE_OFF_LOG)
                    .setPersisted(true)
                    .setUpdateCurrent(true)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            networkManager.schedule(periodic);
            Log.v(getClass().getSimpleName(), "oneoff task scheduled");
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "scheduling failed");
            e.printStackTrace();
        }*/
    }

    public void scheduleRepeat() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int syncTime = Integer.parseInt(prefs.getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
        final Job locationJob = dispatcher.newJobBuilder()
                .setService(LocationSchedulerService.class)
                .setTag(TAG_TASK_PERIODIC_LOG)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(syncTime - 1, syncTime + 1))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        dispatcher.mustSchedule(locationJob);
        Log.v(getClass().getSimpleName(), "repeating task scheduled");

        /*try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int syncTime = Integer.parseInt(prefs.getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
            PeriodicTask periodic = new PeriodicTask.Builder()
                    .setService(NetworkService.class)
                    .setPeriod(syncTime)
                    .setTag(TAG_TASK_PERIODIC_LOG)
                    .setPersisted(true)
                    .setUpdateCurrent(true)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            networkManager.schedule(periodic);
            Log.v(getClass().getSimpleName(), "repeating task scheduled");
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "scheduling failed");
            e.printStackTrace();
        }*/
    }

    public void cancelOneOff() {
        dispatcher.cancel(TAG_TASK_ONE_OFF_LOG);
    }

    public void cancelRepeat() {
        dispatcher.cancel(TAG_TASK_PERIODIC_LOG);
    }

    public void cancelAll() {
        dispatcher.cancelAll();
        isEnabled = false;
        saveEnabledState();
    }

    public static ServiceScheduler getInstance() {
        return instance;
    }

    public boolean isPeriodic() {
        return isPeriodic;
    }

    public void setPeriodic(boolean periodic) {
        isPeriodic = periodic;
    }

}
