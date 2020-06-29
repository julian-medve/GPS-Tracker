package com.wheremobile.gpstracker.model;

import android.os.Parcel;
import android.os.Parcelable;

public class BatteryModel implements Parcelable {

    private int level;
    private int status;

    public static final Parcelable.Creator<BatteryModel> CREATOR = new Creator<BatteryModel>() {

        @Override
        public BatteryModel[] newArray(int size) {
            return new BatteryModel[size];
        }

        @Override
        public BatteryModel createFromParcel(Parcel source) {
            return new BatteryModel(source);
        }
    };

    public BatteryModel(Parcel pc) {
        level = pc.readInt();
        status = pc.readInt();
    }

    public BatteryModel(int level, int status) {
        this.level = level;
        this.status = status;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(level);
        dest.writeInt(status);
    }

    public int getLevel() {
        return level;
    }

    public int getStatus() {
        return status;
    }
}
