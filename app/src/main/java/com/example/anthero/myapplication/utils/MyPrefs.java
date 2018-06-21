package com.example.anthero.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPrefs {
    private static final String PREF_NAME = "grounp_config";
    public static final String GROUNP_CONTROP = "grounp_control";

    private static MyPrefs myPrefs;
    private SharedPreferences sp;
    private MyPrefs(){}
    public static MyPrefs getInstance(){
        if(myPrefs == null){
            myPrefs = new MyPrefs();
        }
        return myPrefs;
    }
    public MyPrefs initSharedPreferences(Context context){
        //获取SharedPreferences对象
        if(sp == null){
            sp = context.getSharedPreferences(PREF_NAME,
                    Context.MODE_PRIVATE);
        }
        return myPrefs;
    }
    /**
     * 向SharedPreferences中写入String类型的数据
     * @param key
     * @param value
     */
    public void writeString(String key, String value){
        SharedPreferences.Editor editor;
        //获取编辑器对象
        editor = sp.edit();
        //写入数据
        editor.putString(key, value);
        editor.apply();//提交写入的数据
    }

    /**
     * 根据key读取SharedPreferences中的String类型的数据
     * @param key
     * @return
     */
    public String readString(String key){
        return sp.getString(key, "");
    }
    public void onDestory(){
        if (sp != null){
            sp = null;
        }
    }
}
