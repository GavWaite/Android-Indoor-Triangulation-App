package com.ewireless.s1208506.navigationinside;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Author: Gavin Waite
 * The Room API Database Access Object to run SQL queries on the database via Java handles
 * https://developer.android.com/training/data-storage/room/index.html
 *
 * Further queries could be added as this application was extended to more locations
 * The database may become very large so to save loading and analysing overhead, then instead of
 * loading the full database each time with getAll(), a subset could be returned based on relative
 * latitude and longitude to the last known location. Therefore only the relevant reference points
 * would be returned.
 */
@Dao
public interface LocDao {
    // USed to acquire the reference points to perform analysis and positioning
    @Query("SELECT * FROM locdata")
    List<LocData> getAll();

    // Returns the integer number of entries for calculating the new UID for the next point
    @Query("SELECT COUNT(uid) FROM locdata")
    int countEntries();

    // Inserts a reference point with the WiFi readings and interpolated location
    @Insert
    void insertOne(LocData data);

    // Clears the entire database
    @Query("DELETE FROM locdata")
    void deleteAll();
}
