package com.becareful.becareful.data;


import android.provider.BaseColumns;

/**
 * Defines table and column names for the status database
 */
public final class BecarefulDbEntry implements BaseColumns{


    /*
    * Table Name inside the database.
    *
    * We'll only use this table to store recently received data from our server
    *
    * */
    public static final String TABLE_NAME  = "status";





    /*
    * Strings for accessing the columns easily
    *
    * */

    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_PRESENCE = "presence";
    public static final String COLUMN_COORDINATES = "coordinates";
    public static final String COLUMN_GEO_TIMESTAMP = "geotimestamp";


    /*
    Main projection to get from the database
     */
    public static final String[] MAIN_STATUS_PROJECTION = {
            COLUMN_DATE,
            COLUMN_TEMPERATURE,
            COLUMN_PRESENCE,
            COLUMN_COORDINATES,
            COLUMN_GEO_TIMESTAMP
    };

    /*
     * Indexes to access the columns when getting a Cursor back
     * from the SQLite database
     *
     * */
    public static final int INDEX_DATE = 0;
    public static final int INDEX_TEMPERATURE = 1;
    public static final int INDEX_PRESENCE = 2;
    public static final int INDEX_COORDINATES = 3;
    public static final int INDEX_GEO_TIMESTAMP = 4;


}
