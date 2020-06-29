package com.wheremobile.gpstracker.utils;

import android.database.Cursor;

public class CursorUtils {

    public static String getString(String columnName, Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
    }

    public static int getInt(String columnName, Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    public static long getLong(String columnName, Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
    }

    public static double getDouble(String columnName, Cursor cursor) {
        return cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
    }

    public static byte[] getBytes(String columnName, Cursor cursor) {
        return cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
    }

    public static short getShort(String columnName, Cursor cursor) {
        return cursor.getShort(cursor.getColumnIndexOrThrow(columnName));
    }

    public static float getFloat(String columnName, Cursor cursor) {
        return cursor.getShort(cursor.getColumnIndexOrThrow(columnName));
    }

}