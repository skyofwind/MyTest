package com.example.anthero.myapplication.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.antheroiot.mesh.MeshProtocol;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.minterface.BleDealListener;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.utils.ControlUtil;

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
        ControlUtil.getInstance().connectDevice(bleDevice, connectDevice);
    }

    private BleGattCallback connectDevice = new BleGattCallback() {
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
            ControlUtil.getInstance().access(bleDevice, access);
        }

        @Override
        public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {

            gatt.disconnect();
            gatt.close();
        }
    };
    private BleWriteCallback access = new BleWriteCallback() {
        @Override
        public void onWriteSuccess(int current, int total, byte[] justWrite) {
            //写入成功则检查
            ControlUtil.getInstance().checkLoginSuccess(bleDevice, checkLoginSuccess);
        }

        @Override
        public void onWriteFailure(BleException exception) {
            //失败则断开连接
            handler.sendEmptyMessage(0x02);
            Toast.makeText(LoginActvity.this, "登陆验证失败", Toast.LENGTH_SHORT).show();
            BleManager.getInstance().disconnect(bleDevice);
            BleManager.getInstance().disconnectAllDevice();
        }
    };

    private BleReadCallback checkLoginSuccess = new BleReadCallback() {
        @Override
        public void onReadSuccess(byte[] data) {
            if (MeshProtocol.getInstance().checkLoginResult(data)) {
                //登陆成功
                handler.sendEmptyMessage(0x02);
                //Toast.makeText(LoginActvity.this, "验证成功", Toast.LENGTH_SHORT).show();
                ControlUtil.getInstance().setupNotification(bleDevice, setupNotification);
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
    };

    private BleNotifyCallback setupNotification =  new BleNotifyCallback() {
        @Override
        public void onNotifySuccess() {
            ControlUtil.getInstance().requestStatus(bleDevice, new byte[]{1}, requestStatus);
        }

        @Override
        public void onNotifyFailure(BleException exception) {
            ControlUtil.getInstance().requestStatus(bleDevice, new byte[]{1}, requestStatus);
        }

        @Override
        public void onCharacteristicChanged(byte[] data) {
            ControlUtil.getInstance().formatBleData(data, dealListener);
        }
    };

    private BleDealListener dealListener = new BleDealListener() {
        @Override
        public void onDeal(DeviceStatus status) {
            deviceStatuses.add(status);
        }

        @Override
        public void onDeal(String s) {

        }
    };

    private BleWriteCallback requestStatus = new BleWriteCallback() {
        @Override
        public void onWriteSuccess(int current, int total, byte[] justWrite) {

        }

        @Override
        public void onWriteFailure(BleException exception) {

        }
    };

}
