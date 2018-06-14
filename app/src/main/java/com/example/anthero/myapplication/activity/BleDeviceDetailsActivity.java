package com.example.anthero.myapplication;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.clj.fastble.data.BleDevice;
import com.example.anthero.myapplication.activity.BaseActvity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BleDeviceDetailsActivity extends BaseActvity {

    @BindView(R.id.device_list)
    RecyclerView deviceList;
    private BleDevice bleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bledevice_layout);
        ButterKnife.bind(this);

    }

    private void getData() {
        bleDevice = (BleDevice) getIntent().getParcelableExtra("device");
    }
}
