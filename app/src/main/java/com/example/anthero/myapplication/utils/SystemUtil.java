package com.example.anthero.myapplication.utils;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class SystemUtil {
    public static int WIDTH = 0;
    public static int HEIGHT = 0;//屏幕除去状态栏和标题栏外的高度
    public static int STATUS_BAR_HEIGHT = 0;//状态栏高度
    public static int MAX_WIDTH = 0;//屏幕最大宽度
    public static int MAX_HEIGHT = 0;//屏幕最大高度
    /**
     * 获取屏幕的宽和高
     * @param context
     * 					参数为上下文对象Context
     * @return
     * 			返回值为长度为2int型数组,其中
     * 			int[0] -- 表示屏幕的宽度
     * 			int[1] -- 表示屏幕的高度
     */
    public static void getSystemDisplay(Context context){
        //创建保存屏幕信息类
        DisplayMetrics dm = new DisplayMetrics();
        //获取窗口管理类
        WindowManager wm =  (WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE);
        //获取屏幕信息并保存到DisplayMetrics中
        wm.getDefaultDisplay().getMetrics(dm);
        //声明数组保存信息
        //int[] displays = new int[2];
        MAX_WIDTH = dm.widthPixels;//屏幕宽度(单位:px)
        MAX_HEIGHT = dm.heightPixels;//屏幕高度

        int statusBarHeight1 = -1;
        //获取status_bar_height资源的ID
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight1 = context.getResources().getDimensionPixelSize(resourceId);
            STATUS_BAR_HEIGHT = statusBarHeight1;
        }
        // 获取标题栏和状态栏高度
        //标题栏默认高度为56dp
        HEIGHT = dm.heightPixels-statusBarHeight1;

        Log.d("HEIGHT",""+HEIGHT);
        Log.d("STATUS_BAR_HEIGHT",""+STATUS_BAR_HEIGHT);
        Log.d("MAX_HEIGHT",""+MAX_HEIGHT);
        Log.d("MAX_WIDTH",""+MAX_WIDTH);
    }
    //dp转px
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    //px转dp
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    //获取Color资源
    public static int getColor(Context context, int id){
        if(Build.VERSION.SDK_INT >= 23){
            return context.getColor(id);
        }else {
            return context.getResources().getColor(id);
        }
    }
}
