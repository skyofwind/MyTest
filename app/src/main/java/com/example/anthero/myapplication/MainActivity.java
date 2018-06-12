package com.example.anthero.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends BaseActvity {

    private final static String TAG = "MainActivity";
    //android6.0需要使用的权限声明
    private final int SDK_PERMISSION_REQUEST = 127;
    private String permissionInfo;
    private ListView blueToothList;
    private BlutoothListAdapter blutoothListAdapter;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
      @Override
      public void handleMessage(Message message){
          switch (message.what){
              case 0x01:
                  statrProgressDialog();
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
        setContentView(R.layout.activity_main);
        init();

    }
    //初始化
    private void init(){
        Button button = (Button)findViewById(R.id.start);
        blueToothList = (ListView)findViewById(R.id.blue_tooth_list);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCheck();
            }
        });

    }
    //检查权限
    private void isCheck(){
        if(Build.VERSION.SDK_INT >= 23){
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                getPersimmions();
            }
        }else {

        }
    }
    //蓝牙设备初始化
    private void initBlueTooth(){

    }

    //检索蓝牙设备
    private void sea

    //log
    private void print(String msg){
        Log.i(TAG,msg);
    }
    //权限相关
    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();

            //
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
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
            if (shouldShowRequestPermissionRationale(permission)){
                return true;
            }else{
                permissionsList.add(permission);
                return false;
            }

        }else{
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == SDK_PERMISSION_REQUEST){
            for(int i = 0; i < permissions.length; ++i){
                print(permissions[i]+" : "+grantResults[i]);
            }
        }

    }
}
