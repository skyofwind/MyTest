/*
 * Copyright (C) 2015 The Telink Bluetooth Light Project
 *
 */
package com.telink.util;

import android.util.Log;

import java.nio.charset.Charset;

public final class Strings {

    private Strings() {
    }

    public static boolean stringIsMac(String val) {
        if (val == null) {
            return false;
        }
        Log.e("Strings", "stringIsMac: "+val);
        String trueMacAddress = "([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}";
        // 这是真正的MAV地址；正则表达式；
        if (val.matches(trueMacAddress)) {
            return true;
        } else {
            return false;
        }
    }

    public static byte[] stringToBytes(String str, int length) {

        byte[] srcBytes;

        if (length <= 0) {
            return str.getBytes(Charset.defaultCharset());
        }

        byte[] result = new byte[length];

        srcBytes = str.getBytes(Charset.defaultCharset());

        if (srcBytes.length <= length) {
            System.arraycopy(srcBytes, 0, result, 0, srcBytes.length);
        } else {
            System.arraycopy(srcBytes, 0, result, 0, length);
        }

        return result;
    }

    public static byte[] stringToBytes(String str) {
        return stringToBytes(str, 0);
    }

    public static String bytesToString(byte[] data) {
        return data == null || data.length <= 0 ? null : new String(data, Charset.defaultCharset()).trim();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
