package com.wheremobile.gpstracker.activity;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.wheremobile.gpstracker.PolicyManager;
import com.wheremobile.gpstracker.R;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.fragment.MainFragment;
import com.wheremobile.gpstracker.receive.ConnectionReceive;
import com.wheremobile.gpstracker.receive.SyncAlarmReceiver;
import com.wheremobile.gpstracker.schedular.ServiceScheduler;
import com.wheremobile.gpstracker.service.SynchronizeService;
import com.wheremobile.gpstracker.service.TurnOnGpsServiceNew;
import com.wheremobile.gpstracker.service.UserLocationService;

import java.io.DataOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import static com.wheremobile.gpstracker.utils.AccountUtils.createSyncAccount;

public class DashboardActivity extends BaseActivity {
    private static final String TAG = DashboardActivity.class.getSimpleName();
    private Context mContext;
    private String[] permissions = {Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_SYNC_SETTINGS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};
    private TelephonyManager telephonyManager;
    private ConnectionReceive connectionReceive = new ConnectionReceive();
    Timer timer;
    TTask tTask;
    private PolicyManager policyManager;
    private boolean isDeviceAdmin = false;
    private ServiceScheduler serviceScheduler;
    public static final String ACTION_ALARM_RUN = "action.alarm.run.run";
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private MainFragment fragment;

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected int getFragmentId() {
        return R.id.content_main;
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mContext = this;
        policyManager = new PolicyManager(this);

        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);
        }
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        //make fully Android Transparent Status bar

        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        initializeTelephonyManager();
        if (null == savedInstanceState) {
            fragment = new MainFragment();
            addFragment(fragment);
        }
        if (checkPermissions(permissions, Constants.REQUEST_PERMISSION_MULTITASK)) {
            startService();
        }
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("DashboardActivity", "permission not granted");
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes("pm grant " + DashboardActivity.this.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS \n");
                os.writeBytes("exit\n");
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), " Device not rooted...", Toast.LENGTH_SHORT).show();
            }

            *//*timer = new Timer();
            tTask = new DashboardActivity.TTask();
            timer.schedule(tTask, 10000, 5000);*//*
        } else {
            Log.d("DashboardActivity", "permission granted");
        }*/

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Intent intent = new Intent(DashboardActivity.this, TurnOnGpsServiceNew.class);
//                intent.putExtra("toTurnOnGps", true);
//                startService(intent);
//            }
//        }, 30000);

        /*ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.WRITE_SECURE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 108);*/
    }

    private void checkDeviceAdmin() {
        if (!policyManager.isAdminActive()) {
            Intent activateDeviceAdmin = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    policyManager.getAdminComponent());
            activateDeviceAdmin
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "After activating admin, you will be able to block application uninstallation.");
            startActivityForResult(activateDeviceAdmin,
                    PolicyManager.DPM_ACTIVATION_REQUEST_CODE);
        } else {
            isDeviceAdmin = true;
            //checkSuPermission();
            /*Toast.makeText(this, "Your Application is not Admin.", Toast.LENGTH_SHORT).show();
            finish();*/
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (resultCode == Activity.RESULT_OK
                && requestCode == PolicyManager.DPM_ACTIVATION_REQUEST_CODE) {
            // handle code for successfull enable of admin
            //MyPreference.getInstance(this).saveBoolean(Const.isAdmin, true);
            isDeviceAdmin = true;
            //checkSuPermission();
            /*PackageManager pm = getPackageManager();
            ComponentName componentName = new ComponentName(this, DashboardActivity.class);
            // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
            pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);*/

        } else {
            Toast.makeText(this, "You deny permission of Admin", Toast.LENGTH_SHORT).show();
            super.onActivityResult(requestCode, resultCode, data);
        }

        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            PackageManager pm = getPackageManager();
            ComponentName componentName = new ComponentName(this, DashboardActivity.class);
            // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
            pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(connectionReceive, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        // checkDeviceAdmin();//temp

        /*PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName(this, DashboardActivity.class);
        // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);*/
    }

    @Override
    protected void onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(connectionReceive);
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_PERMISSION_MULTITASK:
                if (verifyPermissions(grantResults)) {
                    startService();
                    fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
        }
    }

    private void initializeTelephonyManager() {
        if (null == telephonyManager) {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    private void startService() {

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String packageName = getPackageName();
            final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {

                try {
                    final Intent intent = new Intent();
                    String manufacturer = android.os.Build.MANUFACTURER;
                    if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                        intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                    } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                        intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                    } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                        //intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                        //intent.setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.MainGuideActivity"));
                        intent.setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                    }

                    startActivity(intent);

                    List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (list.size() > 0) {
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                final Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }


            *//*if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                final Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);



                try {
                    final Intent intent = new Intent();
                    String manufacturer = android.os.Build.MANUFACTURER;
                    if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                        intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                    } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                        intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                    } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                        intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                    }



                    List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (list.size() > 0) {
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                final Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }*//*
        }*/
/*boolean falg=false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (UserLocationService.class.getName().equals(service.service.getClassName())) {
                falg=true;
            }
        }
        if(!falg) {*/
        Account account = createSyncAccount(this);
        Intent locationIntent = new Intent(this, UserLocationService.class);
        locationIntent.putExtra("extra.account", account);
        startService(locationIntent);
        //  }
        Intent intent1 = new Intent(this, SyncAlarmReceiver.class);
        intent1.setAction(SynchronizeService.ACTION_ALARM_RUN);
        boolean alarmUp = (PendingIntent.getBroadcast(this, 1001,
                intent1,
                PendingIntent.FLAG_NO_CREATE) != null);
        if (alarmUp) {
            Log.i("myTag", "Alarm is already active");
            return;
        }
        Intent i = new Intent(this, SynchronizeService.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long synchTime = Long.parseLong(prefs.getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
        i.setAction(SynchronizeService.ACTION_SET_ALARM);
        if (!UserLocationService.isRepeated)
            i.putExtra(SynchronizeService.EXTRA_TIME, Long.valueOf("210"));
        else i.putExtra(SynchronizeService.EXTRA_TIME, synchTime);
        startService(i);
        //startAlarmRestart();
        /*serviceScheduler = ServiceScheduler.getInstance();
        if (serviceScheduler == null)
            serviceScheduler = ServiceScheduler.newInstance(this);
        if (serviceScheduler != null && !serviceScheduler.isEnabled())
            serviceScheduler.startService(false);*/
    }
   /* private void startAlarmRestart() {

       // Intent intent1 = new Intent(ACTION_ALARM_RUN);
        Account account = createSyncAccount(this);
        Intent alarmservice= new Intent(this, UserLocationService.class);
        alarmservice.putExtra("extra.account", account);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pendingIntent = PendingIntent.getBroadcast(this, 1005, alarmservice, PendingIntent.FLAG_UPDATE_CURRENT);

        boolean alarmUp = (pendingIntent != null);

        if (alarmUp) {
            alarmManager.cancel(pendingIntent);
            Log.i("myTag", "Alarm is already active");
        }
        Log.i("myTag", "pending intent start");
        //for every 2 hrs app receiver
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 1000 * 2 * 60 * 60, pendingIntent);

        // for test set 20 second to restart service
       // alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 20* 1000, pendingIntent);

    }*/

    private void startAlarmRestart() {

        Intent intent1 = new Intent(ACTION_ALARM_RUN);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pendingIntent = PendingIntent.getBroadcast(this, 1005, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

        boolean alarmUp = (pendingIntent != null);

        if (alarmUp) {
            alarmManager.cancel(pendingIntent);
            Log.i("myTag", "Alarm is already active");
        }

        //for every 2 hrs app receiver
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 1000 * 1 * 60 * 60, pendingIntent);

    }

    class TTask extends TimerTask {


        boolean isPermissionGranted;


        @Override
        public void run() {
            isPermissionGranted = ActivityCompat.checkSelfPermission(DashboardActivity.this, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
            Log.d("DashboardActivity", "run timerTask");
            if (isPermissionGranted) {
                Intent intent = new Intent(DashboardActivity.this, TurnOnGpsServiceNew.class);
                intent.putExtra("toTurnOnGps", true);
                startService(intent);
                if (timer != null) {
                    timer.cancel();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }
    }


    @Override
    protected void onStop() {
        /*if (timer != null) {
            timer.cancel();
        }*/
        super.onStop();
    }

    private void checkSuPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("DashboardActivity", "permission not granted");
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes("pm grant " + DashboardActivity.this.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS \n");
                os.writeBytes("exit\n");
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), " Device not rooted...", Toast.LENGTH_SHORT).show();
            }

            /*timer = new Timer();
            tTask = new DashboardActivity.TTask();
            timer.schedule(tTask, 10000, 5000);*/
        } else {

            /*PackageManager pm = getPackageManager();
            ComponentName componentName = new ComponentName(this, DashboardActivity.class);
            // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
            pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Log.e("DashboardActivity", "permission granted");*/
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
