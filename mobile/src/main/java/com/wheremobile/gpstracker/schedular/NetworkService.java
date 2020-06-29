package com.wheremobile.gpstracker.schedular;

import android.content.Intent;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;


public class NetworkService extends GcmTaskService {

    private ServiceScheduler serviceScheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        serviceScheduler = ServiceScheduler.getInstance();
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();

        serviceScheduler = ServiceScheduler.getInstance();
        if (serviceScheduler == null) {
            serviceScheduler = ServiceScheduler.newInstance(this);
        }
        serviceScheduler.startService(false);
    }


    @Override
    public int onRunTask(TaskParams taskParams) {

        final String tag = taskParams.getTag();

        if (tag.equalsIgnoreCase(ServiceScheduler.TAG_TASK_ONE_OFF_LOG) || tag.equalsIgnoreCase(ServiceScheduler.TAG_TASK_PERIODIC_LOG)) {
            final Intent uploadDataIntent = new Intent(this, UploadServerDataIntentService.class);
            startService(uploadDataIntent);

            return GcmNetworkManager.RESULT_SUCCESS;
        } else return GcmNetworkManager.RESULT_FAILURE;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i1) {
        return super.onStartCommand(intent, i, i1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*if (serviceScheduler != null) {
            serviceScheduler.cancelAll();
            serviceScheduler = null;
        }*/
    }
}
