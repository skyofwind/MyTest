package com.example.anthero.myapplication.modle;

import com.clj.fastble.data.BleDevice;

public class MyBleDevice {
    private int id;
    private BleDevice bleDevice;
    private String password;

    public BleDevice getBleDevice() {
        return bleDevice;
    }

    public void setBleDevice(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
