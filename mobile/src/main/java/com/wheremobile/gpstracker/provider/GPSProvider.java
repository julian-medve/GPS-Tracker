package com.wheremobile.gpstracker.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wheremobile.gpstracker.BuildConfig;
import com.wheremobile.gpstracker.db.DBHelper;
import com.wheremobile.gpstracker.model.GPSModel.GPSContract;

import java.util.ArrayList;

public class GPSProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final int GPS_LIST = 0;
    private static final int GPS_ID = 1;

    private static final int LOCATION_LIST = 2;
    private static final int LOCATION_ID = 3;

    private static final UriMatcher URI_MATCHER;

    private DBHelper dbHelper = null;
    private final ThreadLocal<Boolean> isInBatchMode = new ThreadLocal<>();

    private Context context;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        URI_MATCHER.addURI(AUTHORITY, "gps", GPS_LIST);
        URI_MATCHER.addURI(AUTHORITY, "gps/#", GPS_ID);

        URI_MATCHER.addURI(AUTHORITY, "location", LOCATION_LIST);
        URI_MATCHER.addURI(AUTHORITY, "location/#", LOCATION_ID);
    }

    @Override
    public boolean onCreate() {
        context = getContext();
        dbHelper = DBHelper.getInstance(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(uri)) {
            case GPS_LIST:
                builder.setTables(GPSContract.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = GPSContract.SORT_ORDER_DEFAULT;
                }
                break;
            case GPS_ID:
                builder.setTables(GPSContract.TABLE_NAME);
                builder.appendWhere(GPSContract._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(context.getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case GPS_LIST:
                return GPSContract.CONTENT_TYPE;
            case GPS_ID:
                return GPSContract.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        switch (URI_MATCHER.match(uri)) {
            case GPS_LIST:
                id = db.insert(GPSContract.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }
        return getUriForId(id, uri);
    }

    private Uri getUriForId(long id, @NonNull Uri uri) {
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            if (!isInBatchMode()) {
                context.getContentResolver().notifyChange(itemUri, null);
            }
            return itemUri;
        }
        throw new SQLException("Problem while inserting into uri: " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int delCount;
        switch (URI_MATCHER.match(uri)) {
            case GPS_LIST:
                delCount = db.delete(GPSContract.TABLE_NAME, selection, selectionArgs);
                break;
            case GPS_ID:
                String idStr = uri.getLastPathSegment();
                String where = GPSContract._ID + " = " + idStr;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                }
                delCount = db.delete(GPSContract.TABLE_NAME, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        if (delCount > 0 && !isInBatchMode()) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return delCount;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        isInBatchMode.set(true);
        db.beginTransaction();
        try {
            final ContentProviderResult[] retResult = super.applyBatch(operations);
            db.setTransactionSuccessful();
            context.getContentResolver().notifyChange(CONTENT_URI, null);
            return retResult;
        } finally {
            isInBatchMode.remove();
            db.endTransaction();
        }
    }

    private boolean isInBatchMode() {
        return null != isInBatchMode.get() && isInBatchMode.get();
    }
}
