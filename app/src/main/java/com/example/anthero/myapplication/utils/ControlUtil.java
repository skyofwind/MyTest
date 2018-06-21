package com.example.anthero.myapplication.utils;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.antheroiot.mesh.CommandFactory;
import com.antheroiot.mesh.MeshProtocol;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.anthero.myapplication.database.BleDBDao;
import com.example.anthero.myapplication.minterface.BleDealListener;
import com.example.anthero.myapplication.modle.DeviceAccount;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.reciver.BleControlReciver;
import com.telink.util.ArraysUtils;

import java.util.List;

public class ControlUtil {

    private static ControlUtil controlUtil;
    private static String TAG = "ControlUtil";

    private ControlUtil(){

    }

    public static ControlUtil getInstance(){
        if(controlUtil == null){
            controlUtil = new ControlUtil();
        }
        return controlUtil;
    }

    public void power(BleDevice bleDevice, final int meshAddress, boolean onoff) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("myGrouptest","powe方法"+meshAddress+" "+onoff);
        byte[] onOffCmd = CommandFactory.power(meshAddress, onoff).getData(true);
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_COMMAND.toString(),
                onOffCmd,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        Log.d("myGrouptest", "onWriteSuccess "+current+" "+total+" "+ArraysUtils.bytesToHexString(justWrite, ","));
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        Log.d("myGrouptest","onWriteFailure"+exception.toString());
                    }
                }
        );
    }
    public void write(BleDevice bleDevice, String service, String write, byte[] bytes){
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BleManager.getInstance().write(
                bleDevice,
                service,
                write,
                bytes,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        Log.d("write", "onWriteSuccess "+current+" "+total+" "+ArraysUtils.bytesToHexString(justWrite, ","));
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        Log.d("write","onWriteFailure"+exception.toString());
                    }
                }
        );
    }
    public void ota(final Context context, final BleDevice bleDevice, final String service, final String write, final List<byte[]> bytes){
        BleManager.getInstance().write(
                bleDevice,
                service,
                write,
                bytes.get(0),
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        Log.d("ota", "onWriteSuccess "+current+" "+total+" "+ArraysUtils.bytesToHexString(justWrite, ","));
                        bytes.remove(0);
                        Intent intent = new Intent();
                        intent.setAction(BleControlReciver.BLE_CONTROL);
                        intent.putExtra("now", bytes.size());
                        context.sendBroadcast(intent);
                        if(bytes.size() > 0){
                            ota(context, bleDevice, service, write, bytes);
                        }
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        Log.d("ota","onWriteFailure"+exception.toString());
                    }
                }
        );
    }

    public void changeSingleDeviceMesh(final Context context, final List<BleDevice> bleDeviceList, final String service, final String write, final DeviceAccount account, final BleDealListener listener, final List<byte[]> bytes){

        BleManager.getInstance().write(
                bleDeviceList.get(0),
                service,
                write,
                bytes.get(0),
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        log("changeSingleDeviceMesh"+ " onWriteSuccess "+current+" "+total+" "+ArraysUtils.bytesToHexString(justWrite, ",")+System.currentTimeMillis());
                        bytes.remove(0);
                        if(bytes.size() > 0){
                            changeSingleDeviceMesh(context, bleDeviceList, service, write, account, listener, bytes);
                        }else {
                            log("单个完成后"+System.currentTimeMillis()+"");
                            BleManager.getInstance().disconnect(bleDeviceList.get(0));
                            bleDeviceList.remove(0);
                            boolean du;
                            if(du = BleDBDao.getInstance(context).updateBle(account)){
                                Log.e("datebaseupdate", ""+du);
                            }
                            log("单个完成"+System.currentTimeMillis()+"");
                            changeDeviceListMesh(context, bleDeviceList, service, write, account, listener);
                        }
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        log("changeSingleDeviceMesh"+" onWriteFailure"+exception.toString()+System.currentTimeMillis());
                    }
                }
        );
    }
    public void changeDeviceListMesh(final Context context, final List<BleDevice> bleDeviceList, final String service, final String write, final DeviceAccount account, final BleDealListener listener){
        if(bleDeviceList.size() > 0){
            final String name = bleDeviceList.get(0).getMac();
            account.setMac(bleDeviceList.get(0).getMac());
            mConnect(context, bleDeviceList, service, write, account, listener);

        }else{
            listener.onDeal("修改完成");
        }

    }
    public void changeColor(BleDevice bleDevice, int meshAddress, int color) {
        byte[] colorCmd = CommandFactory.setColor(meshAddress, color).getData(true);
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_COMMAND.toString(),
                colorCmd,
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
    public void changeBrightness(BleDevice bleDevice, int meshAddress, float y, int w) {
        byte[] lightCmd = CommandFactory.setLight(meshAddress, y, w).getData(true);
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_COMMAND.toString(),
                lightCmd,
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

    public void connectDevice(BleDevice bleDevice, BleGattCallback callback) {
        if(!BleManager.getInstance().isConnected(bleDevice)){
            BleManager.getInstance().connect(bleDevice.getMac(), callback);
            log("connectDevice");
        }else {
            log("!connectDevice");
        }
    }

    //连接成功后，向蓝牙设备写入之前的登陆信息
    public void access(BleDevice bleDevice, BleWriteCallback callback) {
        byte[] bytes = MeshProtocol.getInstance().getLoginPacket();
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_PAIR.toString(),
                bytes,
                callback
        );
    }

    //检查登陆成功的情况
    public void checkLoginSuccess(BleDevice bleDevice, BleReadCallback callback) {
        BleManager.getInstance().read(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_PAIR.toString(),
                callback
        );
    }
    public void setupNotification(BleDevice bleDevice, BleNotifyCallback callback) {
        BleManager.getInstance().notify(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_STATUS.toString(),
                callback
        );
    }

    public void requestStatus(BleDevice bleDevice, byte[] bytes, BleWriteCallback callback) {
        BleManager.getInstance().write(
                bleDevice,
                MeshProtocol.SERVICE_MESH.toString(),
                MeshProtocol.CHARA_STATUS.toString(),
                bytes,
                callback
        );
    }
    //对数据包进行解密
    public void formatBleData(byte[] bytes, BleDealListener listener) {
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
            _handleStatusData(data, listener);
        }
    }

    public void _handleStatusData(byte[] params, BleDealListener listener) {
        if (params == null || params.length != 20) {
            return;
        }
        if (params[10] != 0) {
            byte[] device = {params[10], params[11], params[12], params[13], params[14]};
            _analysisStatusData(device, listener);
        }
        if (params[15] != 0) {
            byte[] device = {params[15], params[16], params[17], params[18], params[19]};
            _analysisStatusData(device, listener);
        }
    }

    /**
     * @param bytes 长度为5
     */
    public void _analysisStatusData(byte[] bytes, BleDealListener listener) {
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
            listener.onDeal(status);
        }
    }

    private void log(String s){
        Log.e(TAG, s);
    }

    private void mConnect(final Context context, final List<BleDevice> bleDevices, final String service, final String write, final DeviceAccount account, final BleDealListener listener) {
        if(BleManager.getInstance().isConnected(bleDevices.get(0))){
            log(bleDevices.get(0).getMac()+"已连接");
        }else {
            final BleDevice mBleDvice = bleDevices.get(0);
            MeshProtocol.getInstance().preLogin(account.getMac(), account.getOldName(), account.getOldPassword());
            log("prelogin = "+account.getMac()+" "+account.getOldName()+" "+account.getOldPassword());
            BleGattCallback connectDevice = new BleGattCallback() {
                @Override
                public void onStartConnect() {//开始连接
                    log("开始连接设备" + mBleDvice.getMac());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectFail(BleDevice bleDevice, BleException exception) {//连接失败
                    log("连接设备失败" + mBleDvice.getMac()+" "+exception.toString());
                    return;
                }

                @Override
                public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {//连接成功
                    //连接成功后，向设备发送登陆验证
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log("连接设备成功" + mBleDvice.getMac());
                    mAccess(context, bleDevices, service, write, account, listener);
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                    log("设备断开连接" + mBleDvice.getMac());
                    gatt.disconnect();
                    gatt.close();
                    return;
                }
            };
            ControlUtil.getInstance().connectDevice(mBleDvice, connectDevice);//连接设备
        }

    }

    private void mAccess(final Context context, final List<BleDevice> bleDevices, final String service, final String write, final DeviceAccount account, final BleDealListener listener) {
        final BleDevice mBleDvice = bleDevices.get(0);
        BleWriteCallback access = new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                //写入成功则检查
                log("设备access成功" + mBleDvice.getMac());
                mCheckLogin(context, bleDevices, service, write, account, listener);
            }

            @Override
            public void onWriteFailure(BleException exception) {
                //失败则断开连接
                log("设备access失败" + mBleDvice.getMac());
                BleManager.getInstance().disconnectAllDevice();
                return;
            }
        };
        ControlUtil.getInstance().access(bleDevices.get(0), access);
    }


    private void mCheckLogin(final Context context, final List<BleDevice> bleDevices, final String service, final String write, final DeviceAccount account, final BleDealListener listener) {
        final BleDevice mBleDvice = bleDevices.get(0);
        BleReadCallback checkLoginSuccess = new BleReadCallback() {
            @Override
            public void onReadSuccess(byte[] data) {
                if (MeshProtocol.getInstance().checkLoginResult(data)) {
                    //登陆成功
                    log("设备checkLogin成功" + mBleDvice.getMac());
//                    BleManager.getInstance().disconnect(bleDevices.get(0));
//                    bleDevices.remove(0);
                    log("checkLoginResult(data)");
                    List<byte[]> bytes = MeshProtocol.getInstance().configureMesh(account.getNewName(), account.getNewPassword());
                    changeSingleDeviceMesh(context, bleDevices, service, write, account, listener, bytes);

                } else {
                    log("读取成功，设备checkLoginResult失败" + mBleDvice.getMac());
                }
            }

            @Override
            public void onReadFailure(BleException exception) {
                log("设备onReadFailure成功" + mBleDvice.getMac() + " " + exception.toString());
                return;
            }
        };
        ControlUtil.getInstance().checkLoginSuccess(mBleDvice, checkLoginSuccess);
    }

}
