package com.example.anthero.myapplication;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.anthero.myapplication.activity.BaseActvity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceManagerActivity extends BaseActvity {

    @BindView(R.id.devices)
    RecyclerView devices;
    private DeviceManagerRecyclerAdapter adapter;
    private List<DeviceStatus> deviceStatuses;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_device_manager);
        ButterKnife.bind(this);
    }

    private void getData(){

    }

    private void initRecyclerView() {
        adapter = new DeviceManagerRecyclerAdapter(this, deviceStatuses);
        devices.setLayoutManager(new GridLayoutManager(this, 3));
        devices.setAdapter(adapter);
        //devices.addItemDecoration(new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL_LIST));
    }
}
