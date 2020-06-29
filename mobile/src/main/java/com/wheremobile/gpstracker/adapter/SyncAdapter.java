package com.wheremobile.gpstracker.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.db.wrapper.GPSWrapper;
import com.wheremobile.gpstracker.model.GPSModel;
import com.wheremobile.gpstracker.model.GPSModel.GPSContract;
import com.wheremobile.gpstracker.provider.GPSProvider;
import com.wheremobile.gpstracker.utils.AccountUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public SyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        ContentResolver.requestSync(AccountUtils.createSyncAccount(context), GPSProvider.AUTHORITY, bundle);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        if (ContentResolver.isSyncPending(account, authority) || ContentResolver.isSyncActive(account, authority)) {
            Log.i("ContentResolver", "SyncPending, canceling");
            ContentResolver.cancelSync(account, authority);
            return;
        }
        try {
            ContentProvider contentProvider = provider.getLocalContentProvider();
            Cursor cursor = contentProvider.query(GPSContract.CONTENT_URI, GPSContract.PROJECTION_ALL, null, null, null, null);
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
            sendJson(account, gpsList, provider, syncResult);
        } catch (RemoteException | JSONException e) {
            e.printStackTrace();
            Log.e("syncAdapter", "RemoteException  : " + e.toString());
        }
    }

    private void sendJson(Account account, List<GPSModel> gpsList, ContentProviderClient provider, SyncResult syncResult) throws JSONException, RemoteException {
        if (gpsList.size() == 0) return;
        JSONObject jsonObj = new JSONObject();
        JSONArray dataArr = new JSONArray();
        jsonObj.put("imei", account.name);
        for (GPSModel gpsModel : gpsList) {
            JSONObject dataObj = new JSONObject();
            dataObj.put("id", gpsModel.getId());
            dataObj.put("date", gpsModel.getDate().toString());

            JSONObject locationObj = new JSONObject();

            locationObj.put("lat", (double) gpsModel.getLatitude());
            locationObj.put("long", (double) gpsModel.getLongitude());
            locationObj.put("altitude", (double) gpsModel.getAltitude());
            locationObj.put("accuracy", (float) gpsModel.getAccuracy());
            locationObj.put("speed", (float) gpsModel.getSpeed());
            String bearing = gpsModel.getBearing();
            locationObj.put("bearing", TextUtils.isEmpty(bearing) ? String.format(Locale.getDefault(), "%s | %s", "", gpsModel.getStatus()) :
                    String.format(Locale.getDefault(), "%s | %s", bearing, gpsModel.getStatus()));
            locationObj.put("provider", gpsModel.getProvider());
            dataObj.put("location", locationObj);

            dataObj.put("timezone", gpsModel.getTimeZone());
            dataObj.put("battery", gpsModel.getBatteryLevel());
            String phone = gpsModel.getPhone();
            dataObj.put("phone", TextUtils.isEmpty(phone) ? JSONObject.NULL : phone);
            dataObj.put("charging", gpsModel.getCharging());

            dataArr.put(dataObj);
            jsonObj.put("data", dataArr);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();

        FormBody.Builder formBody = new FormBody.Builder()
                .add("response", jsonObj.toString());

        try {
            Response response = client.newCall(new Request.Builder()
                    .url(Constants.URL_SERVER)
                    .method("POST", formBody.build())
                    .build()).execute();

            Log.e("syncAdapter", "URL : " + Constants.URL_SERVER);
            Log.e("syncAdapter", "Json : " + jsonObj.toString());
            JSONObject result = getJSON(response.body().byteStream());
            Log.e("syncAdapter", "adapter : " + result.toString());
            JSONArray resultArr = result.getJSONArray("ids");

            Log.e("SyncAdapter", "json : " + result.toString());
            /*int length = resultArr.length();
            for (int i = 0; i < length; i++) {
                long id = resultArr.getInt(i);
                Uri deleteUri = ContentUris.withAppendedId(GPSContract.CONTENT_URI, id);
                int deleteId = provider.delete(deleteUri, null, null);
                Log.d("logba", "sendJson: deleteId=" + deleteId);
            }*/

            for (GPSModel gps : gpsList) {
                long id = gps.getId();
                Uri deleteUri = ContentUris.withAppendedId(GPSContract.CONTENT_URI, id);
                int deleteId = provider.delete(deleteUri, null, null);
                Log.d("logba", "sendJson: deleteId=" + deleteId);
            }
        } catch (IOException e) {
           // syncResult.stats.numIoExceptions++;
            e.printStackTrace();
            Log.e("syncAdapter", "IOException  : " + e.toString());
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
            Log.e("syncAdapter", "IOException ex : " + ex.toString());
        } finally {
            try {
                if (null != reader) {
                    reader.close();
                }
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
        return new JSONObject(json.toString());
    }
}
