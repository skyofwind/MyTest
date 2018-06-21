package com.antheroiot.utils;

import android.util.Log;

/**
 * Created by Administrator on 2017/11/1
 */

public class RegularExpression {

    public static boolean stringIsMac(String val) {
        if (val == null) {
            return false;
        }
        Log.e("Strings", "stringIsMac: " + val);
        String trueMacAddress = "([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}";
        // 这是真正的MAV地址；正则表达式；
        return val.matches(trueMacAddress);
    }
}
