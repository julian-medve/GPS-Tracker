package com.wheremobile.gpstracker.schedular;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class LocationSchedulerService extends JobService {

    private static final String TAG = "LocationSchedulerService";
    private ServiceScheduler serviceScheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        serviceScheduler = ServiceScheduler.getInstance();
        if (serviceScheduler == null) {
            serviceScheduler = ServiceScheduler.newInstance(this);
        }

        //serviceScheduler.startService(false);

    }

    @Override
    public boolean onStartJob(JobParameters job) {
        Log.e(TAG, "onStartJob:");
        final String tag = job.getTag();

        if (tag.equalsIgnoreCase(ServiceScheduler.TAG_TASK_ONE_OFF_LOG) || tag.equalsIgnoreCase(ServiceScheduler.TAG_TASK_PERIODIC_LOG)) {
            final Intent uploadDataIntent = new Intent(this, UploadServerDataIntentService.class);
            startService(uploadDataIntent);

            new Handler().postDelayed(() -> {
                Log.e("jobFinished", tag);
                if (!tag.equalsIgnoreCase(ServiceScheduler.TAG_TASK_ONE_OFF_LOG)) {
                    jobFinished(job, true);
                } else jobFinished(job, false);

            }, 2000);

            return true;
        } else return false;

    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Log.e(TAG, "onStopJob:");
        return false;
    }
}
