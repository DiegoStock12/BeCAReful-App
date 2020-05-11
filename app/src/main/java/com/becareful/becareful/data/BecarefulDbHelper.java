package com.becareful.becareful.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BecarefulDbHelper extends SQLiteOpenHelper {

    /** Database name and version**/
    public static final String DATABASE_NAME = "becareful.db";
    private static final int DATABASE_VERSION = 3;


    /** Singleton instance of the database **/
    private static BecarefulDbHelper sInstance;

    public static synchronized BecarefulDbHelper getInstance(Context context){

        if (sInstance == null){
            sInstance = new BecarefulDbHelper(context.getApplicationContext());
        }
        return sInstance;

    }



    private BecarefulDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_STATUS_TABLE =

                "CREATE TABLE " + BecarefulDbEntry.TABLE_NAME +"(" +

                        BecarefulDbEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        BecarefulDbEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                        BecarefulDbEntry.COLUMN_TEMPERATURE + " REAL NOT NULL, " +
                        BecarefulDbEntry.COLUMN_PRESENCE + " INT NOT NULL, " +
                        BecarefulDbEntry.COLUMN_COORDINATES + " TEXT NOT NULL, "+
                        BecarefulDbEntry.COLUMN_GEO_TIMESTAMP + " INTEGER NOT NULL, "+

                        "UNIQUE ("+ BecarefulDbEntry.COLUMN_DATE +") ON CONFLICT REPLACE);";

        db.execSQL(SQL_CREATE_STATUS_TABLE);
        Log.v("HOLA", "Created db if not exists");



    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + BecarefulDbEntry.TABLE_NAME);
        onCreate(db);

    }
}
