package com.example.anthero.myapplication.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BleDBHelper extends SQLiteOpenHelper {
    private final static String TAG = "BleDBHelper";
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "bledevice.db";
    public static final String TABLE_NAME = "bledevice";

    public BleDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        log("FictionDBHelper(Context context)");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        log("onCreate(SQLiteDatabase db)");
        String sql = "create table if not exists " + TABLE_NAME + " (Id integer primary key AUTOINCREMENT, mac text, password text)";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        log("onUpgrade");
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }

    private void log(String s){
        Log.d(TAG, s);
    }
}
