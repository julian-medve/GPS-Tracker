package com.wheremobile.gpstracker.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract @LayoutRes int getLayout();
    protected abstract @IdRes int getFragmentId();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
    }

    public void showMessage(@StringRes int resource) {
        showMessage(getString(resource), null, Snackbar.LENGTH_LONG);
    }

    public void showMessage(@NonNull String message) {
        showMessage(message, null, Snackbar.LENGTH_LONG);
    }

    public void showMessage(@StringRes int resource, View.OnClickListener listener) {
        showMessage(getString(resource), listener, Snackbar.LENGTH_LONG);
    }

    public void showMessage(@StringRes int resource, View.OnClickListener listener, int length) {
        showMessage(getString(resource), listener, length);
    }

    public void showMessage(@NonNull final String message, View.OnClickListener listener, int length) {
        Snackbar.make(findViewById(getFragmentId()), message, length)
                .setAction(android.R.string.ok, listener)
                .show();
    }

    boolean checkPermission(@NonNull String permission) {
        return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED;
    }

    boolean requestPermission(@NonNull String permission, int request) {
        if (checkPermission(permission)) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, request);
            return false;
        }
        return true;
    }

    public boolean checkPermissions(@NonNull String[] permissions, int request) {
        final List<String> permissionsList = new ArrayList<>();
        for(String permission : permissions) {
            if (!addPermission(permission)) {
                permissionsList.add(permission);
            }
        }
        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), request);
            return false;
        }
        return true;
    }

    private boolean addPermission(@NonNull String permission) {
        if (checkPermission(permission)) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return false;
            }
        }
        return true;
    }

    public boolean verifyPermissions(@NonNull int[] grantResults) {
        if(grantResults.length < 1) return false;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (null != fragments) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    protected void addFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(getFragmentId(), fragment)
                .commit();
    }

    protected void replaceFragment(Fragment fragment) {
        replaceFragment(fragment, false);
    }

    protected void replaceFragment(Fragment fragment, boolean animation) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (animation) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        }
        transaction.replace(getFragmentId(), fragment)
                .commit();
    }

}