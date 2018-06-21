package com.example.anthero.myapplication.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.antheroiot.utils.MeshBeacon;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.adpater.BlueToothRecyclerAdapter;
import com.example.anthero.myapplication.decoration.DividerItemDecoration;
import com.example.anthero.myapplication.utils.ThreadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpdateMeshActivity extends BaseActvity {

    @BindView(R.id.start)
    Button start;
    @BindView(R.id.blue_tooth_list)
    RecyclerView blueToothList;
    @BindView(R.id.device_name)
    EditText deviceName;
    private BlueToothRecyclerAdapter blutoothListAdapter;
    private Timer timer;
    private List<BleDevice> bleDevices;
    private BleDevice mBleDvice;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0x01:
                    String text = message.obj.toString();
                    statrProgressDialog(text);
                    break;
                case 0x02:
                    cancel();
                    break;
                case 0x03:
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    break;
                case 0x04:
                    clear();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_mesh_layout);
        ButterKnife.bind(this);
        setmTAG("UpdateMeshActivity");
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!ThreadUtil.isEmpty()) {
            ThreadUtil.getInstance().destory();
        }
    }

    //初始化
    private void init() {
        initRecyclerView();
        initBlueTooth();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                        .setScanTimeOut(5000)
                        .setDeviceName(true, deviceName.getText().toString())
                        .build();
                BleManager.getInstance().initScanRule(scanRuleConfig);
                isCheck();
            }
        });
    }

    private void initRecyclerView() {
        bleDevices = new ArrayList<>();
        blutoothListAdapter = new BlueToothRecyclerAdapter(this, bleDevices);
        blutoothListAdapter.setType(1);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        blueToothList.setLayoutManager(layoutManager);
        blueToothList.setAdapter(blutoothListAdapter);
        blueToothList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
    }

    //检查权限
    private void isCheck() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                getPersimmions();
            } else {
                //initBlueTooth();
                searchBlueTooth();
            }

        } else {
            searchBlueTooth();
        }
    }

    //蓝牙设备初始化
    private void initBlueTooth() {
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setOperateTimeout(5000);
    }

    //检索蓝牙设备
    private void searchBlueTooth() {
        //判断是否支持蓝牙功能
        if (BleManager.getInstance().isSupportBle()) {
            //蓝牙是否打开
            if (!BleManager.getInstance().isBlueEnable()) {
                //打开蓝牙
                BleManager.getInstance().enableBluetooth();
                Message message = new Message();
                message.what = 0x01;
                message.obj = "正在开启蓝牙中";
                handler.sendMessage(message);
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (BleManager.getInstance().isBlueEnable()) {
                            handler.sendEmptyMessage(0x02);
                            handler.sendEmptyMessage(0x03);
                            search();
                        }
                    }
                }, 100, 100);
            } else {
                search();
            }
        }
    }

    //正式搜索
    private void search() {
        handler.sendEmptyMessage(0x04);
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                if (success) {
                    Message m = new Message();
                    m.obj = "正在搜索蓝牙设备";
                    m.what = 0x01;
                    handler.sendMessage(m);
                } else {
                    Toast.makeText(UpdateMeshActivity.this, "搜索失败请重新尝试", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {

            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                if (bleDevice != null) {
                    MeshBeacon beacon = new MeshBeacon();
                    beacon.load(bleDevice.getMac(), bleDevice.getName(), bleDevice.getRssi(), bleDevice.getScanRecord());
                    if (beacon.companyId == 0x0211 && beacon.isEncryptDevice()) {//公司ID和是否加密
                        int position = blutoothListAdapter.addDevice(bleDevice);
                        blueToothList.smoothScrollToPosition(position);
                    }
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                handler.sendEmptyMessage(0x02);
            }
        });
    }

    //
    private void clear() {
        bleDevices.clear();
        blutoothListAdapter.notifyDataSetChanged();
    }
}
