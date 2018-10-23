package com.ewireless.s1208506.navigationinside;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Author: Gavin Waite
 * The Room API Database Entity definition. This defines the columns of each record/entry in the
 * database and their types
 * https://developer.android.com/training/data-storage/room/index.html
 */
@Entity(tableName = "locdata")
public class LocData {
    // Simply used to order the entries and guarantee their uniqueness
    @PrimaryKey
    public int uid;

    // The interpolated location of the reference point
    @ColumnInfo
    public double latitude;
    @ColumnInfo
    public double longitude;

    // The 3 strongest WiFi access points from the scan at the reference point
    // This could be extended to have more access points to tune the positioning algorithm further
    @ColumnInfo
    public String BSSID_1;
    @ColumnInfo
    public int dB_1;

    @ColumnInfo
    public String BSSID_2;
    @ColumnInfo
    public int dB_2;

    @ColumnInfo
    public String BSSID_3;
    @ColumnInfo
    public int dB_3;

    // EMF Support was dropped as it was found to be inaccurate compared to WiFi strength and was
    // dependent on device orientation
//    @ColumnInfo(name = "emfX")
//    public float emfX;
//    @ColumnInfo(name = "emfY")
//    public float emfY;
//    @ColumnInfo(name = "emfZ")
//    public float emfZ;
}

