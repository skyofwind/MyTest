package com.example.anthero.myapplication.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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
import com.example.anthero.myapplication.minterface.BleDealListener;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.reciver.BleControlReciver;
import com.example.anthero.myapplication.utils.Config;
import com.example.anthero.myapplication.utils.ControlUtil;
import com.example.anthero.myapplication.view.RainbowPalette;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BleDeviceControlActivity extends BaseActvity implements RainbowPalette.OnColorChangedListen, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    @BindView(R.id.tv_choose_pre)
    TextView txtChoosePre;
    @BindView(R.id.tv_choose_next)
    TextView txtChooseNext;
    @BindView(R.id.imv_led_palettle)
    RainbowPalette rainbowPalette;
    @BindView(R.id.rl_led_paletle)
    RelativeLayout rlLedPaletle;
    @BindView(R.id.tv_set_y_alpha)
    TextView tvSetYAlpha;
    @BindView(R.id.id_temperature_alpha_seek_bar)
    SeekBar temperatureSeekBar;
    @BindView(R.id.led_y_light_layout)
    LinearLayout ledYLightLayout;
    @BindView(R.id.tv_set_w_alpha)
    TextView tvSetWAlpha;
    @BindView(R.id.id_brightness_alpha_seek_bar)
    SeekBar brightnessSeekBar;
    @BindView(R.id.led_w_light_layout)
    LinearLayout ledWLightLayout;

    private int currentIndex = 6;//当前的颜色的index，默认为蓝色
    private int currentColor;//选中的颜色
    private String currentRGB;
    private SparseArray sparseArray;
    private int[] ledNormalColor = {0xFFFFFFFF, 0xFFFF0000, 0xFFF3990C, 0xFFEEF60B, 0xFF3C981B, 0xFF3CE2F3, 0xFF0511FB, 0xFFAB56EE};
    private BleDevice bleDevice;
    private int meshAddress;
    private static int total = 1000;

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
        setContentView(R.layout.device_control_layout);
        ButterKnife.bind(this);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!BleManager.getInstance().isConnected(bleDevice)){
            ControlUtil.getInstance().connectDevice(bleDevice, connectDevice);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initViews() {
        txtChoosePre.setOnClickListener(this);
        txtChooseNext.setOnClickListener(this);
        rainbowPalette.setOnChangeListen(this);
        brightnessSeekBar.setOnSeekBarChangeListener(this);
        temperatureSeekBar.setOnSeekBarChangeListener(this);
    }

    private void getData(){
        bleDevice = (BleDevice)getIntent().getParcelableExtra(BleControlReciver.DEVICE);
        meshAddress = getIntent().getIntExtra(BleControlReciver.MESH_ADDRESS, 0);
    }

    private void init() {
        getData();
        initViews();
        initColorPosition();
    }

    /**
     * 手动设置指示小球显示的位置
     */
    public void setIndictorPosition(int index) {
        if (sparseArray != null) {
            rainbowPalette.setIndictorPosition((Point) sparseArray.get(ledNormalColor[index]));
            rainbowPalette.setCenterPaint((ledNormalColor[currentIndex]));
        }
        RainbowPalette.isNeedShowIndictor = false;
        rainbowPalette.invalidate();
    }

    private void onChooseColor(View view) {
        switch (view.getId()) {
            case R.id.tv_choose_pre:
                setIndictorPosition(choosePreColor());
                break;
            case R.id.tv_choose_next:
                setIndictorPosition(chooseNextColor());
                break;
            default:
                break;
        }
    }

    /**
     * 选择前一种颜色，并返回对应的索引
     *
     * @return
     */
    private int choosePreColor() {
        int size = ledNormalColor.length;
        if (currentIndex == 0) {
            currentIndex = size;
        }
        currentIndex = currentIndex - 1;
        currentColor = ledNormalColor[currentIndex];
        return currentIndex;
    }

    /**
     * 选择后一种颜色，并返回对应的索引
     *
     * @return
     */
    private int chooseNextColor() {
        int size = ledNormalColor.length;
        if (currentIndex == size - 1) {
            currentIndex = -1;
        }
        currentIndex = currentIndex + 1;
        currentColor = ledNormalColor[currentIndex];
        return currentIndex;
    }

    /**
     * 初始化固定颜色对应的坐标值
     */
    private void initColorPosition() {

        Point[] points = {new Point(-75, 1), new Point(110, -2), new Point(102, -88), new Point(57, -120),
                new Point(-69, -131), new Point(-145, -18), new Point(-64, 102), new Point(44, 103)};
        sparseArray = new SparseArray<String>();
        for (int i = 0; i < points.length; i++) {

            sparseArray.append(ledNormalColor[i], points[i]);
        }
    }

    /**
     * 获取argb模式的颜色值并转为字符串
     *
     * @param color
     * @return
     */
    private String parseArgb(int color) {
        int a = (color >>> 24);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        return String.valueOf(a) + String.valueOf(r) + String.valueOf(g) + String.valueOf(b);
    }

    private String parseRGB(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        return String.valueOf(r) + String.valueOf(g) + String.valueOf(b);
    }


    @Override
    public void onColorChange(int color) {
        //txtTitle.setTextColor(color);
        currentColor = color;
        currentRGB = Integer.toHexString(color);
        if(Config.controlType){
            ControlUtil.getInstance().changeColor(bleDevice, Config.groupId, color);
        }else {
            ControlUtil.getInstance().changeColor(bleDevice, meshAddress, color);
        }



        Log.e("Color", "onColorChange: " + Color.red(color) + Color.green(color) + Color.blue(color));
        Log.e("Color", "onColorChange:parseArgb " + parseArgb(color));
        Log.e("Color", "onColorChange:parseRGB " + parseRGB(color));
        Log.e("Color", "onColorChange: " + Integer.toHexString(color));//获取十进制字符串表示argb模式的颜色0xFFF3990C-->fff3990c
    }

    /**
     * 获取最终的颜色值ARGB模式的
     *
     * @param progress
     * @return
     */
    private int getChangedColor(int progress) {
        String red, green, blue;
        if (progress == 0) {
            progress = 1;
        }
        if (currentRGB == null) {
            currentRGB = "FF0511FB";
        }
        red = currentRGB.substring(2, 4);
        green = currentRGB.substring(4, 6);
        blue = currentRGB.substring(6);
        return Color.argb(progress, Integer.parseInt(red, 16), Integer.parseInt(green, 16), Integer.parseInt(blue, 16));
    }

    @Override
    public void onClick(View v) {
        onChooseColor(v);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        switch (seekBar.getId()){
            case R.id.id_temperature_alpha_seek_bar:
                Log.d("Color", "色温: " + progress);
                if(Config.controlType){
                    ControlUtil.getInstance().changeBrightness(bleDevice, Config.groupId, getColorTemperature(), getColorBrightness());
                }else {
                    ControlUtil.getInstance().changeBrightness(bleDevice, meshAddress, getColorTemperature(), getColorBrightness());
                }
                break;
            case R.id.id_brightness_alpha_seek_bar:
                Log.d("Color", "亮度: " + progress);
                if(Config.controlType){
                    ControlUtil.getInstance().changeBrightness(bleDevice, Config.groupId, getColorTemperature(), getColorBrightness());
                }else {
                    ControlUtil.getInstance().changeBrightness(bleDevice, meshAddress, getColorTemperature(), getColorBrightness());
                }
                break;
        }
        Log.d("Color", "亮度: " + getColorBrightness()+"\n色温"+ getColorTemperature());
        //currentColor = getChangedColor(progress);
        //rainbowPalette.setCenterPaint(currentColor);
        Log.e("Color", "onProgressChanged: " + Integer.toHexString(currentColor));
        Log.e("Color", "onProgressChanged: " + Integer.toHexString(currentColor));
        //rainbowPalette.invalidate();
        //txtTitle.setTextColor(currentColor);

        Log.e("Color", "onProgressChanged: rgb" + Color.red(currentColor) + Color.green(currentColor) + Color.blue(currentColor));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        return;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        return;
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
            Toast.makeText(BleDeviceControlActivity.this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BleDeviceControlActivity.this, "登陆验证失败", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(BleDeviceControlActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onReadFailure(BleException exception) {
            handler.sendEmptyMessage(0x02);
            Toast.makeText(BleDeviceControlActivity.this, "读取失败", Toast.LENGTH_SHORT).show();
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
    //获取色温比例的值
    private float getColorTemperature(){
        double temp = (double) temperatureSeekBar.getProgress()/total;
        Log.d("myColorss", "temp = "+temp+" progress = "+temperatureSeekBar.getProgress());
        return (float) temp;
    }
    //获取亮度的值
    private int getColorBrightness(){
        return brightnessSeekBar.getProgress();
    }


}
