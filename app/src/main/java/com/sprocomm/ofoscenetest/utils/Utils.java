package com.sprocomm.ofoscenetest.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.JsonReader;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuanbin.ning on 2017/6/7.
 */

public class Utils {
    public static final  String ERROR_CONNECT_TOAST = "error_connect_toast";
    public static final  String SEND_OK = "send_ok";
    public static final  int ERROR_CONNECT_TOAST_CMD = 0x1;
    public static final  int SEND_OK_CMD= 0x2;
    public static final String SEND_BROADCAST_FOR_BUFFER = "send_buffer";
    public static final String ACTION_BROADCAST_IN_ACITIVITY= "com.sprocomm.ofoScene.test1";
    public static final String ACTION_BROADCAST_IN_SERVICE = "com.sprocomm.ofoScene.test2";

    public static String bcd2Str(byte[] bytes) {
        StringBuffer temp = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
            temp.append((byte) (bytes[i] & 0x0f));
        }
        return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp
                .toString().substring(1) : temp.toString();
    }

    public static String asciiToStr(byte[] value) {
        StringBuffer sbu = new StringBuffer();
        for (int i = 0; i < value.length; i++) {
            sbu.append((char) value[i]);
        }
        return sbu.toString();
    }

    public static boolean compare(int value, int min, int max) {
        if (min == max) {
            if (value == min) {
                return true;
            } else {
                return false;
            }
        } else if (min < max) {
            if (value < min || value > max) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean compare(int value, String min_max) {
        if (!TextUtils.isEmpty(min_max)) {
            char[] array = min_max.trim().toCharArray();
            int index = 0;
            for (int i = 0; i < array.length; ++i) {
                try {
                    Integer.parseInt(String.valueOf(array[i]));
                } catch (NumberFormatException e) {
                    index = i;
                    break;
                }
            }
            try {
                int min = Integer.parseInt(min_max.trim().substring(0, index));
                int max = Integer.parseInt(min_max.trim().substring(index + 1,
                        array.length));
                return compare(value, min, max);
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    public static byte[] getHexBytes(String message) {
        int len = message.length() / 2;
        char[] chars = message.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
    }
}
