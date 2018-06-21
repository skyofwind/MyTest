package com.example.anthero.myapplication.database;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.example.anthero.myapplication.modle.DeviceAccount;
import com.example.anthero.myapplication.modle.MyBleDevice;

import java.util.ArrayList;
import java.util.List;

public class BleDBDao {
    private static final String TAG = "FictionDao";
    private BleDBHelper bleDBHelper;
    private Context context;
    private final String[] BLE_COLUMNS = new String[] {"Id", "mac", "password"};
    private static BleDBDao bleDBDao;

    public static BleDBDao getInstance(Context context){
        if(bleDBDao == null){
            bleDBDao = new BleDBDao(context);
        }
        return bleDBDao;
    }

    private BleDBDao(Context context){
        this.context = context;
        bleDBHelper = new BleDBHelper(context);
    }

    public List<MyBleDevice> getAllData(){
        log("getAllData()");
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = bleDBHelper.getReadableDatabase();
            // select * from Orders
            cursor = db.query(BleDBHelper.TABLE_NAME, BLE_COLUMNS, null, null, null, null, null);

            if (cursor.getCount() > 0) {
                List<MyBleDevice> bleList = new ArrayList<MyBleDevice>(cursor.getCount());
                while (cursor.moveToNext()) {
                    bleList.add(parseBleDevice(cursor));
                }
                return bleList;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "", e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return null;
    }

    public int insertBles(List<MyBleDevice> devices){
        int i = 0;
        if (devices != null){
            for(MyBleDevice device : devices){
                if(insertBle(device)){
                    i++;
                }
            }
        }
        return i;
    }

    public boolean insertBle(MyBleDevice myBleDevice){
        if(isExist(myBleDevice)){
            return false;
        }
        log("insertFiction");
        SQLiteDatabase db = null;
        try {
            db = bleDBHelper.getWritableDatabase();
            db.beginTransaction();
            ContentValues contentValues = new ContentValues();
            contentValues.put(BLE_COLUMNS[1], myBleDevice.getBleDevice().getMac());
            contentValues.put(BLE_COLUMNS[2], myBleDevice.getPassword());
            db.insertOrThrow(BleDBHelper.TABLE_NAME, null, contentValues);
            db.setTransactionSuccessful();
            return true;
        }catch (SQLiteConstraintException e){
            //tip("主键重复");
        }catch (Exception e){
            Log.e(TAG, "", e);
        }finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
        return false;
    }

    public boolean deleteBle(int id){
        log("deleteFiction");
        SQLiteDatabase db = null;
        try {
            db = bleDBHelper.getWritableDatabase();
            db.beginTransaction();
            db.delete(BleDBHelper.TABLE_NAME, "Id = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
        return false;
    }

    public boolean updateBle(DeviceAccount account){
        MyBleDevice device = getIdByMac(account.getMac());
        device.setPassword(account.getNewPassword());
        return updateBle(device);
    }

    public boolean updateBle(MyBleDevice myBleDevicet){
        log("updateBle");
        SQLiteDatabase db = null;
        try {
            db = bleDBHelper.getWritableDatabase();
            db.beginTransaction();
            ContentValues contentValues = new ContentValues();
            contentValues.put(BLE_COLUMNS[1], myBleDevicet.getBleDevice().getMac());
            contentValues.put(BLE_COLUMNS[2], myBleDevicet.getPassword());
            db.update(BleDBHelper.TABLE_NAME, contentValues,"Id = ?", new String[]{String.valueOf(myBleDevicet.getId())});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
        return false;
    }

    public MyBleDevice getMacById(int id){
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = bleDBHelper.getReadableDatabase();
            // select * from Orders
            cursor = db.query(BleDBHelper.TABLE_NAME, BLE_COLUMNS, "Id = ? ", new String[]{id+""}, null, null, null);
            cursor.moveToFirst();
            return parseBleDevice(cursor);
        }
        catch (Exception e) {
            Log.e(TAG, "", e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return null;
    }

    public MyBleDevice getIdByMac(String mac){
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = bleDBHelper.getReadableDatabase();
            // select * from Orders
            cursor = db.query(BleDBHelper.TABLE_NAME, BLE_COLUMNS, "mac = ? ", new String[]{mac}, null, null, null);
            cursor.moveToFirst();
            return parseBleDevice(cursor);
        }
        catch (Exception e) {
            Log.e(TAG, "", e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return null;
    }

    public Boolean isExist(MyBleDevice myBleDevice){
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = bleDBHelper.getReadableDatabase();
            // select * from Orders
            cursor = db.query(BleDBHelper.TABLE_NAME, BLE_COLUMNS, "mac = ?", new String[]{myBleDevice.getBleDevice().getMac()}, null, null, null);
            if(cursor.getCount() > 0){
                return true;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "", e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return false;
    }
    public MyBleDevice parseBleDevice(Cursor cursor){
        MyBleDevice myBleDevice = new MyBleDevice();
        String mac = cursor.getString(cursor.getColumnIndex(BLE_COLUMNS[1]));
        BluetoothDevice bluetoothDevice = BleManager.getInstance().getBluetoothAdapter().getRemoteDevice(mac);
        myBleDevice.setBleDevice(BleManager.getInstance().convertBleDevice(bluetoothDevice));
        myBleDevice.setId(cursor.getInt(cursor.getColumnIndex(BLE_COLUMNS[0])));
        myBleDevice.setPassword(cursor.getString(cursor.getColumnIndex(BLE_COLUMNS[2])));
        return myBleDevice;
    }
    private void log(String s){
        Log.d(TAG, s);
    }
}
