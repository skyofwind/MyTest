package com.example.anthero.myapplication.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
import com.example.anthero.myapplication.activity.DeviceManagerActivity;
import com.example.anthero.myapplication.database.BleDBDao;
import com.example.anthero.myapplication.minterface.BleDealListener;
import com.example.anthero.myapplication.modle.DeviceAccount;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.modle.MyBleDevice;
import com.example.anthero.myapplication.utils.ControlUtil;
import com.example.anthero.myapplication.utils.SystemUtil;
import com.example.anthero.myapplication.utils.ThreadUtil;

import java.util.List;

public class LoginDialogFragment extends DialogFragment {
    private View root;
    private EditText deviceName, password;
    private Button login, back;
    private BleDevice bleDevice;
    private Dialog progressDialog;
    private boolean  progress=false;
    private Context mContext;
    private int type = 0;
    private List<BleDevice> deviceList;
    private DeviceAccount account = new DeviceAccount();
    private static String lastName = "", lastPsw = "";

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
                case  0x03:
                    Toast.makeText(mContext, message.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = inflater.inflate(R.layout.login_layout, null);
        getData();
        init();
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = SystemUtil.MAX_WIDTH-50;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        //params.y = 30;
        window.setAttributes(params);
        //设置背景透明
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancel();
    }

    private void getData(){
        Bundle bundle = getArguments();
        if(bundle != null){
            bleDevice = bundle.getParcelable("device");
            type = bundle.getInt("type", 0);
        }
    }

    private void init(){
        login = (Button)root.findViewById(R.id.login);
        back = (Button)root.findViewById(R.id.back);
        deviceName = (EditText)root.findViewById(R.id.device_name);
        password = (EditText)root.findViewById(R.id.password);

        if(type == 2){
            resetUpdate();
        }else {
            if(bleDevice != null){
                if (bleDevice.getName() != null){
                    deviceName.setText(bleDevice.getName());
                }else {
                    deviceName.setText("none");
                }
            }
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    private void login() {
        Log.d("UpdateTest", "type = "+type);
        if(type != 2){
            MeshProtocol.getInstance().preLogin(bleDevice.getMac(), bleDevice.getName(), password.getText().toString());//
            lastName = bleDevice.getName();
            lastPsw = password.getText().toString();
            ControlUtil.getInstance().connectDevice(bleDevice, connectDevice);//连接设备
        }
        if (type == 2){
            if (password.getText().toString().equals("")) {
                Toast.makeText(mContext, "密码为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if(deviceName.getText().toString().equals("")){
                Toast.makeText(mContext, "密码为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if(deviceList != null){
                Message message = new Message();
                message.what = 0x01;
                message.obj = "修改信息中";
                handler.sendMessage(message);
                ThreadUtil.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        resetMesh(deviceList);
                    }
                });
            }
        }

    }
    private void statrProgressDialog(String text){
        if(progressDialog == null){
            progressDialog = new Dialog(mContext, R.style.progress_dialog);
            progressDialog.setContentView(R.layout.dialog);
            progressDialog.setCancelable(false);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        TextView msg = (TextView) progressDialog.findViewById(R.id.id_tv_loadingmsg);
        msg.setText(text);
        progress=true;
        progressDialog.show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if(type == 1 || type == 2){
            BleManager.getInstance().stopNotify(bleDevice,
                    MeshProtocol.SERVICE_MESH.toString(),
                    MeshProtocol.CHARA_STATUS.toString());
            BleManager.getInstance().disconnectAllDevice();
            Log.d("disconnectTest", "dismiss() called"+BleManager.getInstance().isConnected(bleDevice));
        }
    }

    private void cancel(){
        if(progress && progressDialog != null){
            progress=false;
            progressDialog.dismiss();
            Log.d("cancel", " cancel");
        }
    }

    private void resetUpdate(){
        deviceName.setEnabled(true);
        deviceName.setHint("请输入要修改的名字");
        password.setHint("请输入要修改的密码");
        login.setText("修改");
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
            Toast.makeText(mContext, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, "登陆验证失败", Toast.LENGTH_SHORT).show();
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
                //ControlUtil.getInstance().setupNotification(bleDevice, setupNotification);
                switch (type){
                    case 0:
                        Intent intent=new Intent(mContext,DeviceManagerActivity.class);
                        Bundle bundle=new Bundle();
                        bundle.putParcelable("device",bleDevice);
                        intent.putExtras(bundle);
                        dismiss();
                        MyBleDevice myBleDevice=new MyBleDevice();
                        myBleDevice.setBleDevice(bleDevice);
                        myBleDevice.setPassword(password.getText().toString());
                        BleDBDao.getInstance(mContext).insertBle(myBleDevice);
                        mContext.startActivity(intent);
                        break;
                    case 1:
                        BleManager.getInstance().disconnectAllDevice();
                        LoginDialogFragment loginDialogFragment=new LoginDialogFragment();
                        Bundle bundle2=new Bundle();
                        bundle2.putParcelable("device",bleDevice);
                        bundle2.putInt("type",2);
                        loginDialogFragment.setArguments(bundle2);
                        if(deviceList != null){
                            loginDialogFragment.setDeviceList(deviceList);
                        }
                        loginDialogFragment.show(getActivity().getFragmentManager(),"login");

                        myBleDevice=new MyBleDevice();
                        myBleDevice.setBleDevice(bleDevice);
                        myBleDevice.setPassword(password.getText().toString());
                        BleDBDao.getInstance(mContext).insertBle(myBleDevice);
                        dismiss();

                        break;
                    case 2:

                        break;
                }
            } else {
                handler.sendEmptyMessage(0x02);
                Toast.makeText(mContext, "验证失败", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onReadFailure(BleException exception) {
            handler.sendEmptyMessage(0x02);
            Toast.makeText(mContext, "读取失败", Toast.LENGTH_SHORT).show();
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

    public void setDeviceList(List<BleDevice> deviceList) {
        this.deviceList = deviceList;
    }
    private void resetMesh(List<BleDevice> bleDeviceList){

        account.setMac(bleDevice.getMac());
        account.setNewName(deviceName.getText().toString());
        account.setNewPassword(password.getText().toString());
        account.setOldName(lastName);
        account.setOldPassword(lastPsw);
        BleDealListener bleDealListener = new BleDealListener() {
            @Override
            public void onDeal(DeviceStatus status) {

            }

            @Override
            public void onDeal(String s) {
                handler.sendEmptyMessage(0x02);
                Message message = new Message();
                message.what = 0x03;
                message.obj = s;
                handler.sendMessage(message);
                dismiss();
            }
        };
        ControlUtil.getInstance().changeDeviceListMesh(mContext,
                bleDeviceList, MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_PAIR.toString(),
                account,
                bleDealListener);
    }

    private void log(String s){
        Log.e("LoginDialogFragment", s);
    }
}
