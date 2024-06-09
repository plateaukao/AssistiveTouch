package com.android.mirror.assisttouch.utils;

import java.io.DataOutputStream;

public class KeyEventsSender {

    public static void sendKeyEvent(String keyCode) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());

            // 发送 input keyevent 命令来模拟按键事件
            outputStream.writeBytes("input keyevent " + keyCode + "\n");
            outputStream.flush();

            // 结束 su 进程
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
