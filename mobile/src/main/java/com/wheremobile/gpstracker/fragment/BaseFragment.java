package com.wheremobile.gpstracker.fragment;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import com.wheremobile.gpstracker.activity.BaseActivity;

public class BaseFragment extends Fragment {

    protected BaseActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (BaseActivity) context;
    }

    public void showMessage(@StringRes int resource) {
        showMessage(getString(resource), null, Snackbar.LENGTH_LONG);
    }

    public void showMessage(String message) {
        showMessage(message, null, Snackbar.LENGTH_LONG);
    }

    public void showMessage(@StringRes int resource, View.OnClickListener listener) {
        showMessage(getString(resource), listener, Snackbar.LENGTH_LONG);
    }

    public void showMessage(@StringRes int resource, View.OnClickListener listener, int length) {
        showMessage(getString(resource), listener, length);
    }

    public void showMessage(final String message, View.OnClickListener listener, int length) {
        activity.showMessage(message, listener, length);
    }

    void checkPermissions(String[] permissions, int request) {
        activity.checkPermissions(permissions, request);
    }

    public boolean verifyPermissions(@NonNull int[] grantResults) {
        return activity.verifyPermissions(grantResults);
    }
}
