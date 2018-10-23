package com.ewireless.s1208506.navigationinside;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Author: Gavin Waite
 * Implementation of a Room database of LocData entities
 * LocDao provides the access methods
 * https://developer.android.com/training/data-storage/room/index.html
 *
 * The @Database annotation defines the database settings to use LocData for each entry in the table
 * It also links the Database Access Object (DAO) which is defined in a separate file
 */
@Database(entities = {LocData.class}, version = 1)
public abstract class LocationDatabase extends RoomDatabase {

    public abstract LocDao locDao();

}
