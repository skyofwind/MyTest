package com.example.anthero.myapplication.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.antheroiot.mesh.CommandFactory;
import com.antheroiot.mesh.MeshProtocol;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.adpater.DeviceManagerRecyclerAdapter;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.utils.Config;
import com.example.anthero.myapplication.utils.MyPrefs;
import com.suke.widget.SwitchButton;
import com.telink.util.ArraysUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceManagerActivity extends BaseActvity {

    @BindView(R.id.devices)
    RecyclerView devices;
    @BindView(R.id.grounpButton)
    SwitchButton grounpButton;
    @BindView(R.id.relink)
    Button relink;
    @BindView(R.id.lstart)
    Button lstart;
    @BindView(R.id.lend)
    Button lend;
    private BleDevice bleDevice;
    private DeviceManagerRecyclerAdapter adapter;
    private SparseArray<DeviceStatus> deviceStatuses;
    private List<Integer> index;

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
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_device_manager);
        ButterKnife.bind(this);
        getData();
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registReciver();
        if (!BleManager.getInstance().isConnected(bleDevice)) {
            connectDevice(bleDevice);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unRegistReciver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().stopNotify(bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_STATUS.toString());
        BleManager.getInstance().disconnect(bleDevice);
    }

    private void getData() {
        bleDevice = (BleDevice) getIntent().getParcelableExtra("device");
        setupNotification();
    }

    private void init() {

        deviceStatuses = new SparseArray<>();
        index = new ArrayList<>();
        adapter = new DeviceManagerRecyclerAdapter(this, deviceStatuses, index, bleDevice);
        devices.setLayoutManager(new GridLayoutManager(this, 3));
        devices.setAdapter(adapter);

        relink.setOnClickListener(clickListener);
        lstart.setOnClickListener(clickListener);
        lend.setOnClickListener(clickListener);
        grounpButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                MyPrefs.getInstance().writeString(MyPrefs.GROUNP_CONTROP, String.valueOf(isChecked));
                Config.controlType = isChecked;
                adapter.setGroup(Config.controlType);
            }
        });
        grounpButton.setChecked(Config.controlType);
        //devices.addItemDecoration(new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL_LIST));
    }

    private void connectDevice(BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice.getMac(), new BleGattCallback() {
            @Override
            public void onStartConnect() {//开始连接
                Message message = new Message();
                message.what = 0x01;
                message.obj = "蓝牙设备重连中";
                handler.sendMessage(message);
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {//连接失败
                handler.sendEmptyMessage(0x02);
                Toast.makeText(DeviceManagerActivity.this, "蓝牙重连失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {//连接成功
                //连接成功后，向设备发送登陆验证
                handler.sendEmptyMessage(0x02);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message message = new Message();
                message.what = 0x01;
                message.obj = "登陆验证中";
                handler.sendMessage(message);
                access();
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {

                gatt.disconnect();
                gatt.close();
            }
        });
    }

    //连接成功后，向蓝牙设备写入之前的登陆信息
    private void access() {
        byte[] bytes = MeshProtocol.getInstance().getLoginPacket();
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_PAIR.toString(),
                bytes,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        //写入成功则检查
                        checkLoginSuccess();
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        //失败则断开连接
                        handler.sendEmptyMessage(0x02);
                        Toast.makeText(DeviceManagerActivity.this, "登陆验证失败", Toast.LENGTH_SHORT).show();
                        BleManager.getInstance().disconnect(bleDevice);
                        BleManager.getInstance().disconnectAllDevice();
                    }
                }
        );
    }

    //检查登陆成功的情况
    private void checkLoginSuccess() {
        BleManager.getInstance().read(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_PAIR.toString(),
                new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {
                        if (MeshProtocol.getInstance().checkLoginResult(data)) {
                            //登陆成功
                            handler.sendEmptyMessage(0x02);
                            //Toast.makeText(LoginActvity.this, "验证成功", Toast.LENGTH_SHORT).show();
                            setupNotification();

                        } else {
                            handler.sendEmptyMessage(0x02);
                            Toast.makeText(DeviceManagerActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onReadFailure(BleException exception) {
                        handler.sendEmptyMessage(0x02);
                        Toast.makeText(DeviceManagerActivity.this, "读取失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupNotification() {
        BleManager.getInstance().notify(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_STATUS.toString(),
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        requestStatus();
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        requestStatus();
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        formatBleData(data);
                    }
                }
        );
    }

    private void requestStatus() {
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_STATUS.toString(),
                new byte[]{1},
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {

                    }

                    @Override
                    public void onWriteFailure(BleException exception) {

                    }
                }
        );
    }

    public void formatBleData(byte[] bytes) {
        if (bytes.length != 20) {
            return;
        }
        byte[] data = MeshProtocol.getInstance().decryptData(bytes);
//        Log.e(TAG, "notify: " + ArraysUtils.bytesToHexString(bytes, ","));

        byte opcode = data[7];
        int venderIdLow = data[8];
        int venderIdHigh = data[9];
        if (venderIdLow != 0x11 || venderIdHigh != 2) {
            // TODO: 2017/11/6 re-login
            return;
        }
        if (opcode == CommandFactory.Opcode.RESPONSE_DEVICE_LIST) {
            _handleStatusData(data);
        }
    }

    private void _handleStatusData(byte[] params) {
        if (params == null || params.length != 20) {
            return;
        }
        if (params[10] != 0) {
            byte[] device = {params[10], params[11], params[12], params[13], params[14]};
            _analysisStatusData(device);
        }
        if (params[15] != 0) {
            byte[] device = {params[15], params[16], params[17], params[18], params[19]};
            _analysisStatusData(device);
        }
    }

    /**
     * @param bytes 长度为5
     */
    private void _analysisStatusData(byte[] bytes) {
        if (bytes == null || bytes.length != 5) {
            return;
        }
        Log.d("TAG", ArraysUtils.bytesToHexString(bytes, ","));
        int seq = bytes[1] & 0xff;
        int productId = bytes[4] << 8 | bytes[3] & 0xff;//result:4321;
        int state = bytes[2] & 0xff;
        int meshAddr = bytes[0] & 0xff;
//        Log.e(TAG, String.format("pid:0x%x", productId));
        if (productId == 0x4004 || productId == 0x4001 || productId == 0x4321) {
            DeviceStatus status = new DeviceStatus(meshAddr, seq, state, productId);
            if (!index.contains(status.getMeshAddress())) {
                adapter.addIndex(status.getMeshAddress());
                adapter.addDeviceStatus(status, true);
            } else {
                if (adapter.isChange(status)) {
                    log("检测到改变");
                    adapter.addDeviceStatus(status, false);
                } else {
                    log("没检测到改变");
                }
            }
            log(status.toString());
            //deviceStatuses.add(status);
            //adapter.updateDeviceStatus(status);
        }
    }



    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.relink:
                    if (!BleManager.getInstance().isConnected(bleDevice)) {
                        connectDevice(bleDevice);
                    } else {
                        Toast.makeText(DeviceManagerActivity.this, "设备已连接", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.lstart:
                    goToDetatil();
                    break;
                case R.id.lend:

                    break;
            }
        }
    };

    private void goToDetatil(){
        Intent intent = new Intent(this, BleDeviceDetailsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("device", bleDevice);
        intent.putExtras(bundle);
        startActivity(intent);
    }
    private void log(String s) {
        Log.d("DeviceManagerActivity", s);
    }
}