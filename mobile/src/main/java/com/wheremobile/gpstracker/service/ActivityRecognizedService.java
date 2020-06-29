package com.wheremobile.gpstracker.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;


public class ActivityRecognizedService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            final Intent resultIntent = new Intent(UserLocationService.ACTION_DEVICE_MOVEMENT);
            //final ArrayList detectedActivities = (ArrayList) result.getProbableActivities();
            final ArrayList<DetectedActivity> detectedActivities = new ArrayList<DetectedActivity>();
            detectedActivities.add(result.getMostProbableActivity());
            resultIntent.putExtra("data", detectedActivities);
            sendBroadcast(resultIntent);
        }
    }


}
