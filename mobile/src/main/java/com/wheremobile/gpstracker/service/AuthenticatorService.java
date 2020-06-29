package com.wheremobile.gpstracker.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.wheremobile.gpstracker.account.Authenticator;

public class AuthenticatorService extends Service {

    private Authenticator authenticator;

    @Override
    public void onCreate() {
        authenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
