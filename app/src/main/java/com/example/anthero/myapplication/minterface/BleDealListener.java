package com.example.anthero.myapplication.minterface;

import com.example.anthero.myapplication.modle.DeviceStatus;

public interface BleDealListener {
    void onDeal(DeviceStatus status);
    void onDeal(String s);
}
