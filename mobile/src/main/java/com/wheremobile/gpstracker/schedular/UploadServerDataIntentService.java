package com.wheremobile.gpstracker.schedular;

import android.accounts.Account;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.db.wrapper.GPSWrapper;
import com.wheremobile.gpstracker.model.GPSModel;
import com.wheremobile.gpstracker.service.ServiceManager;
import com.wheremobile.gpstracker.service.SynchronizeService;
import com.wheremobile.gpstracker.service.UserLocationService;
import com.wheremobile.gpstracker.utils.AccountUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;


public class UploadServerDataIntentService extends IntentService {

    private static final String TAG = "TrackerIntentService";

    public UploadServerDataIntentService() {
        super("UploadServerDataIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (!checkInternet(this))
            return;

        Account account = AccountUtils.createSyncAccount(this);
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(GPSModel.GPSContract.CONTENT_URI, GPSModel.GPSContract.PROJECTION_ALL, null, null, null, null);
        List<GPSModel> gpsList = new ArrayList<>();
        GPSWrapper gpsWrapper = null;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                do {
                    gpsWrapper = new GPSWrapper(cursor);
                    gpsList.add(gpsWrapper.getGPSModel());
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        try {
            sendJson(account, gpsList, contentResolver);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    boolean checkInternet(Context context) {
        ServiceManager serviceManager = new ServiceManager(context);
        return serviceManager.isNetworkAvailable();
    }

    private String getDateFormat(String pattern, long date) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(date);
    }

    private void sendJson(Account account, List<GPSModel> gpsList, ContentResolver contentResolver) throws JSONException {
        if (gpsList.size() == 0) return;
        JSONObject jsonObj = new JSONObject();
        JSONArray dataArr = new JSONArray();
        jsonObj.put("imei", account.name);
        for (GPSModel gpsModel : gpsList) {
            JSONObject dataObj = new JSONObject();

            Date date = gpsModel.getDate();
            long timeInMilli = date.getTime();

            dataObj.put("id", gpsModel.getId());
            dataObj.put("activity_datetime", getDateFormat(Constants.PATTERN_ACTIVITY_DATETIME, timeInMilli));
            dataObj.put("activity_date", getDateFormat(Constants.PATTERN_ACTIVITY_DATE, timeInMilli));
            dataObj.put("activity_time", getDateFormat(Constants.PATTERN_ACTIVITY_TIME, timeInMilli));
            dataObj.put("date", date.toString());

            JSONObject locationObj = new JSONObject();

            locationObj.put("lat", (double) gpsModel.getLatitude());
            locationObj.put("long", (double) gpsModel.getLongitude());
            locationObj.put("altitude", (double) gpsModel.getAltitude());
            locationObj.put("accuracy", (float) gpsModel.getAccuracy());
            locationObj.put("speed", (float) gpsModel.getSpeed());
            final String bearing = gpsModel.getBearing();
            String bearingStatus = "";
            if (TextUtils.isEmpty(bearing)) {
                bearingStatus = String.format(Locale.getDefault(), "%s | %s", "", gpsModel.getStatus());
            } else if (TextUtils.isEmpty(gpsModel.getStatus())) {
                bearingStatus = String.format(Locale.getDefault(), "%s", bearing);
            } else {
                bearingStatus = String.format(Locale.getDefault(), "%s | %s", bearing, gpsModel.getStatus());
            }

            //bearingStatus = String.format(Locale.getDefault(), "%s", TextUtils.isEmpty(bearing) ? "" : bearing);

            locationObj.put("bearing", bearingStatus);
            locationObj.put("provider", gpsModel.getProvider());
            dataObj.put("location", locationObj);

            dataObj.put("timezone", gpsModel.getTimeZone());
            dataObj.put("battery", gpsModel.getBatteryLevel());
            String phone = gpsModel.getPhone();
            dataObj.put("phone", TextUtils.isEmpty(phone) ? JSONObject.NULL : phone);
            dataObj.put("charging", gpsModel.getCharging());
            //dataObj.put("status", gpsModel.getStatus());

            dataArr.put(dataObj);
            jsonObj.put("data", dataArr);
        }

        // getDateFormat(Constants.PATTERN_ACTIVITY_DATETIME_DEMO, System.currentTimeMillis())
        //generateNoteOnSD(this,"Rushabh", jsonObj.toString());

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();

        FormBody.Builder formBody = new FormBody.Builder()
                .add("response", jsonObj.toString());

        Log.e("SyncAdapter", "json : " + jsonObj.toString());


        try {
            final Response response = client.newCall(new Request.Builder()
                    .url(Constants.URL_SERVER)
                    .method("POST", formBody.build())
                    .build()).execute();

            //temp php server
            /*final Response response = client.newCall(new Request.Builder()
                    .url("http://192.168.1.18:8082/projects/gpsactivity/upload-android/upload-android.php")
                    .method("POST", formBody.build())
                    .build()).execute();*/


            final JSONObject result = getJSON(response.body().byteStream());
            if (result != null) {
                Log.e("SyncAdapter", "json : " + result.toString());
                final JSONArray resultArr = result.optJSONArray("ids");
                if (resultArr != null && resultArr.length() > 0) {
                    final int length = resultArr.length();
                    for (int i = 0; i < length; i++) {
                        long id = resultArr.getInt(i);
                        Uri deleteUri = ContentUris.withAppendedId(GPSModel.GPSContract.CONTENT_URI, id);
                        int deleteId = contentResolver.delete(deleteUri, null, null);
                        Log.d("logba", "sendJson: deleteId=" + deleteId);
                    }
                    gpsList.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*if (!UserLocationService.isRepeated) {
            UserLocationService.isRepeated = true;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            long synchTime = Long.parseLong(prefs.getString(Constants.SETTINGS_SYNCHRONIZE_INTERVAL, "210"));
            Intent i = new Intent(this, SynchronizeService.class);
            i.setAction(SynchronizeService.ACTION_SET_ALARM);
            i.putExtra(SynchronizeService.EXTRA_TIME, synchTime);
            startWakefulService(this, i);
        }*/
        /*ServiceScheduler serviceScheduler = ServiceScheduler.getInstance();

        if (serviceScheduler == null) {
            serviceScheduler = ServiceScheduler.newInstance(this);
        }

        if (serviceScheduler != null && !serviceScheduler.isPeriodic()) {
            serviceScheduler.cancelOneOff();
            serviceScheduler.startService(true);
        }*/
    }

    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
            String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String rootPath = storagePath + "/test";
            String fileName = "/test.zip";
            File root = new File(rootPath, "GPSTrackerData");
            if (!root.exists()) {
                root.getParentFile().mkdirs();
            }
            File gpxfile = new File(root, sFileName + ".json");
            gpxfile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(gpxfile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(sBody);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private JSONObject getJSON(@NonNull InputStream inputStream) throws JSONException {
        StringBuilder json = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while (null != (line = reader.readLine())) {
                json.append(line)
                        .append("\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (null != reader) {
                    reader.close();
                }
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
        Log.d("logba", "SyncAlarmReceiver -> getJSON : " + json.toString());
        return new JSONObject(json.toString());
    }

}
