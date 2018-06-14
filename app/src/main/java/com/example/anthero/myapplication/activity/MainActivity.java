package com.example.anthero.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.antheroiot.utils.MeshBeacon;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.example.anthero.myapplication.activity.BaseActvity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActvity {

    private final static String TAG = "MainActivity";
    //android6.0需要使用的权限声明
    private final int SDK_PERMISSION_REQUEST = 127;
    @BindView(R.id.start)
    Button start;
    @BindView(R.id.blue_tooth_list)
    RecyclerView blueToothList;
    @BindView(R.id.login)
    Button login;
    private String permissionInfo;
    private BlueToothRecyclerAdapter blutoothListAdapter;
    private boolean isInit = false;
    private Timer timer;
    private List<BleDevice> bleDevices;


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
                        print("timer已经取消");
                    }
                    break;
                case 0x04:
                    clear();
                    break;
            }
        }
    };

    @Nullable
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    //初始化
    private void init() {

        initRecyclerView();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCheck();
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, LoginActvity.class));
            }
        });
    }

    private void initRecyclerView() {
        bleDevices = new ArrayList<>();
        blutoothListAdapter = new BlueToothRecyclerAdapter(MainActivity.this, bleDevices);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        blueToothList.setLayoutManager(layoutManager);
        blueToothList.setAdapter(blutoothListAdapter);
        blueToothList.addItemDecoration(new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL_LIST));
    }

    //检查权限
    private void isCheck() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                getPersimmions();
            } else {
                initBlueTooth();
                searchBlueTooth();
            }

        } else {
            initBlueTooth();
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
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setDeviceName(true, "hp")
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
        isInit = true;
    }

    //检索蓝牙设备
    private void searchBlueTooth() {
        //判断是否支持蓝牙功能
        if (BleManager.getInstance().isSupportBle()) {
            //蓝牙是否打开
            if (!BleManager.getInstance().isBlueEnable()) {
                //打开蓝牙
                print("已经打开");
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
                            print("蓝牙已经开启好了");
                            search();
                        }
                        print("timer还在运行");
                    }
                }, 100, 100);
            } else {
                print("还没有");
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
                    Toast.makeText(MainActivity.this, "搜索失败请重新尝试", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                if (bleDevice != null) {
                    //bleDevice.getRssi() 信号强度
                    //bleDevice.getMac() 设备地址
                    //bleDevice.getScanRecord() 设备记录
                    MeshBeacon beacon = new MeshBeacon();
                    beacon.load(bleDevice.getMac(), bleDevice.getName(), bleDevice.getRssi(), bleDevice.getScanRecord());
                    if (beacon.companyId == 0x0211 && beacon.isEncryptDevice()) {//公司ID和是否加密
                        int position = blutoothListAdapter.addDevice(bleDevice);
                        blueToothList.smoothScrollToPosition(position);
                        print("RSSI : " + bleDevice.getRssi());
                    }
                }
            }

            @Override
            public void onScanning(BleDevice bleDevice) {

            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                handler.sendEmptyMessage(0x02);
                blutoothListAdapter.setBackTop(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        blueToothList.smoothScrollToPosition(0);
                    }
                });
            }
        });
    }

    //
    private void clear() {
        bleDevices.clear();
        blutoothListAdapter.notifyDataSetChanged();
    }

    //log
    private void print(String msg) {
        Log.i(TAG, msg);
    }

    //权限相关
    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();

            //
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
             */
            // 读写权限
//            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
//            }
//            // 读取电话状态权限
//            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
//                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
//            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)) {
                return true;
            } else {
                permissionsList.add(permission);
                return false;
            }

        } else {
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SDK_PERMISSION_REQUEST) {
            for (int i = 0; i < permissions.length; ++i) {
                //蓝牙授权成功
                if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == 0) {
                    initBlueTooth();
                    searchBlueTooth();
                }
            }
        }

    }
}
