package com.wheremobile.gpstracker.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.wheremobile.gpstracker.adapter.SyncAdapter;
import com.wheremobile.gpstracker.schedular.UploadServerDataIntentService;
import com.wheremobile.gpstracker.service.ServiceManager;

public class ConnectionReceive extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            if (checkInternet(context)) {
                //SyncAdapter.syncImmediately(context);

                final Intent uploadDataIntent = new Intent(context, UploadServerDataIntentService.class);
                context.startService(uploadDataIntent);

            }
        }
    }

    boolean checkInternet(Context context) {
        ServiceManager serviceManager = new ServiceManager(context);
        return serviceManager.isNetworkAvailable();
    }
}
