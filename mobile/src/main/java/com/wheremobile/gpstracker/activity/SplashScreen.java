package com.wheremobile.gpstracker.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.wheremobile.gpstracker.PolicyManager;
import com.wheremobile.gpstracker.R;

public class SplashScreen extends AppCompatActivity {
    private static final String LOG_TAG = "Splash";
    private PolicyManager policyManager;

    private boolean isBackPressed=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isBackPressed)
                {
                    startActivity(new Intent(SplashScreen.this, DashboardActivity.class));
                    finish();
                }
            }
        },2000);

        //Hide App from Menu
        //temp
//        PackageManager pm = getPackageManager();
//        ComponentName componentName = new ComponentName(this, SplashScreen.class);
//        // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
//        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
//
//        /*if (MyPreference.getInstance(this).getBoolean(Const.isAdmin)) {
//            startActivity(new Intent(SplashScreen.this, DashboardActivity.class));
//            finish();
//        } else*/
//        {
//            policyManager = new PolicyManager(this);
//            if (!policyManager.isAdminActive()) {
//                Intent activateDeviceAdmin = new Intent(
//                        DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//                activateDeviceAdmin.putExtra(
//                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
//                        policyManager.getAdminComponent());
//                activateDeviceAdmin
//                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
//                                "After activating admin, you will be able to block application uninstallation.");
//                startActivityForResult(activateDeviceAdmin,
//                        PolicyManager.DPM_ACTIVATION_REQUEST_CODE);
//            } else {
//                Toast.makeText(this, "Your Application is not Admin.", Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(SplashScreen.this, DashboardActivity.class));
//                finish();
//            }
//        }

        /*if (!MyPreference.getInstance(this).getBoolean(Const.isFirstTime)) */
        /*{
            //Hide App from Menu
            PackageManager pm = getPackageManager();
            ComponentName componentName = new ComponentName(this, SplashScreen.class);
            // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
            pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            //MyPreference.getInstance(this).saveBoolean(Const.isFirstTime, true);
            //Log.e("SplashScreen", "execute FirstTime");
        }*/
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        isBackPressed=true;
        finish();
    }

    //    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // TODO Auto-generated method stub
//        if (resultCode == Activity.RESULT_OK
//                && requestCode == PolicyManager.DPM_ACTIVATION_REQUEST_CODE) {
//            // handle code for successfull enable of admin
//            //MyPreference.getInstance(this).saveBoolean(Const.isAdmin, true);
//        } else {
//            Toast.makeText(this, "You deny permission of Admin", Toast.LENGTH_SHORT).show();
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//        startActivity(new Intent(SplashScreen.this, DashboardActivity.class));
//        finish();
//    }
}
