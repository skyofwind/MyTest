package com.example.anthero.myapplication.reciver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BleControlReciver extends BroadcastReceiver {

    public final static String BLE_CONTROL = "ble_control_reciver";
    public final static String STATUS = "status";
    public final static String DEVICE = "device";
    public final static String MESH_ADDRESS = "meshAddress";
    public final static String ONOFF = "onoff";
    private final static String TAG = "BleControlReciver";

    private BleUpdatelListener updatelListener;
    private int max;
    private int now;


    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(BLE_CONTROL)){
            now = intent.getIntExtra("now", max);
            if(updatelListener != null){
                updatelListener.onUpdateUI(max, now);
            }
        }
    }

    public interface BleUpdatelListener{
        void onUpdateUI(int max, int now);
    }

    public void setPowerListener(BleUpdatelListener bleUpdatelListener) {
        this.updatelListener = bleUpdatelListener;
    }

    public void setMax(int max) {
        this.max = max;
    }

    private void log(String s){
        Log.d(TAG, s);
    }

}
