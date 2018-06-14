package com.example.anthero.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.anthero.myapplication.activity.BaseActvity;
import com.telink.util.ArraysUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActvity extends BaseActvity {

    @BindView(R.id.device_name)
    EditText deviceName;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.login)
    Button login;
    @BindView(R.id.test)
    Button test;

    private BleDevice bleDevice;
    private List<DeviceStatus> deviceStatuses = new ArrayList<>();

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
        setContentView(R.layout.login_layout);
        ButterKnife.bind(this);
        getData();
        init();
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printStatus();
            }
        });
    }

    private void getData() {
        bleDevice = (BleDevice) getIntent().getParcelableExtra("device");
    }

    private void init() {
        deviceName.setText(bleDevice.getName());
    }

    private void login() {
        if (deviceName.getText().toString().equals("")) {
            Toast.makeText(this, "用户名为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.getText().toString().equals("")) {
            Toast.makeText(this, "密码为空", Toast.LENGTH_SHORT).show();
            return;
        }
        MeshProtocol.getInstance().preLogin(bleDevice.getMac(), bleDevice.getName(), password.getText().toString());//
        connectDevice(bleDevice);//连接设备
    }

    private void connectDevice(BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice.getMac(), new BleGattCallback() {
            @Override
            public void onStartConnect() {//开始连接
                Message message = new Message();
                message.what = 0x01;
                message.obj = "正在连接蓝牙设备";
                handler.sendMessage(message);
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {//连接失败
                handler.sendEmptyMessage(0x02);
                Toast.makeText(LoginActvity.this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(LoginActvity.this, "登陆验证失败", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LoginActvity.this, "验证失败", Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onReadFailure(BleException exception) {
                        handler.sendEmptyMessage(0x02);
                        Toast.makeText(LoginActvity.this, "读取失败", Toast.LENGTH_SHORT).show();
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
            deviceStatuses.add(status);
            //adapter.updateDeviceStatus(status);
        }
    }

    private void printStatus() {
        for (DeviceStatus deviceStatus : deviceStatuses) {
            Log.d("myTest", "" + deviceStatus.isChanged(deviceStatus));
        }
    }
}
