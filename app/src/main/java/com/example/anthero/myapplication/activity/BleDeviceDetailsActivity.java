package com.example.anthero.myapplication.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.minterface.BleDealListener;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.reciver.BleControlReciver;
import com.example.anthero.myapplication.utils.ControlUtil;
import com.example.anthero.myapplication.utils.FileUtil;
import com.example.anthero.myapplication.utils.ThreadUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BleDeviceDetailsActivity extends BaseActvity {

    @BindView(R.id.mac)
    TextView mac;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.version)
    TextView version;
    @BindView(R.id.progress)
    NumberProgressBar progress;
    @BindView(R.id.send)
    Button send;

    private BleDevice bleDevice;
    private String password, versionText;
    private BleControlReciver bleControlReciver;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bledevice_layout);
        ButterKnife.bind(this);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registReciver();
        connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegistReciver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void getData() {
        bleDevice = (BleDevice) getIntent().getParcelableExtra("device");
        //password = getIntent().getStringExtra("password");
    }

    private void init() {
        setmTAG("BleDeviceDetailsActivity");
        getData();
        getVersion(bleDevice);
        mac.setText(bleDevice.getMac());
        name.setText(bleDevice.getName());
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadUtil.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            byte[] bytes = FileUtil.rawToByteArray(getResources().openRawResource(R.raw.update));
                            sendData(bytes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void connect() {
        if (!BleManager.getInstance().isConnected(bleDevice)) {
            ControlUtil.getInstance().connectDevice(bleDevice, connectDevice);
        }
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
            Toast.makeText(BleDeviceDetailsActivity.this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BleDeviceDetailsActivity.this, "连接验证失败", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(BleDeviceDetailsActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onReadFailure(BleException exception) {
            handler.sendEmptyMessage(0x02);
            Toast.makeText(BleDeviceDetailsActivity.this, "读取失败", Toast.LENGTH_SHORT).show();
        }
    };

    private BleNotifyCallback setupNotification = new BleNotifyCallback() {
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
            //deviceStatuses.add(status);
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

    private void getVersion(BleDevice bleDevice) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BleManager.getInstance().read(
                bleDevice,
                MeshProtocol.SERVICE_INFO.toString(),
                MeshProtocol.CHARA_FIRMWARE_VERSION.toString(),
                new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {
                        versionText = new String(data);
                        version.setText(versionText);
                    }

                    @Override
                    public void onReadFailure(BleException exception) {

                    }
                });
    }

    private void sendData(byte[] bs) throws Exception {
        List<byte[]> bytes = CommandFactory.pktFirmware(bs);
        if(bleControlReciver != null){
            bleControlReciver.setMax(bytes.size());
        }
        ControlUtil.getInstance().ota(this, bleDevice, MeshProtocol.SERVICE_MESH.toString(), MeshProtocol.CHARA_OTA.toString(), bytes);
    }

    private void registReciver() {
        if (bleControlReciver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BleControlReciver.BLE_CONTROL);
            bleControlReciver = new BleControlReciver();
            registerReceiver(bleControlReciver, filter);
            bleControlReciver.setPowerListener(new BleControlReciver.BleUpdatelListener() {
                @Override
                public void onUpdateUI(int max, int now) {
                    int p = (int) ((1 - (float)now/max)*100);
                    progress.setProgress(p);
                    if(p == 100){
                        Toast.makeText(BleDeviceDetailsActivity.this, "升级包发送成功请等候固件升级", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void unRegistReciver() {
        if (null != bleControlReciver) {
            unregisterReceiver(bleControlReciver);
            bleControlReciver = null;
        }
    }
}
