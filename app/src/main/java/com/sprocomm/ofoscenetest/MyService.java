package com.sprocomm.ofoscenetest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.sprocomm.ofoscenetest.utils.ContastValue;
import com.sprocomm.ofoscenetest.utils.PrefUtils;
import com.sprocomm.ofoscenetest.utils.Utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MyService extends Service {
    private Socket mSocket = null;
    private int cmdStatic;
    private String deviceStatic;
    private GetMainActivityBroadcast acitivtyBroadcast;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_BROADCAST_IN_SERVICE);
       acitivtyBroadcast = new GetMainActivityBroadcast();
        registerReceiver(acitivtyBroadcast, filter);
        Log.d("wjb sprocomm","----------------onStartCommand");
        connect();
        return super.onStartCommand(intent, flags, startId);
    }

    private void receiveMsg() {
        if (mSocket != null) {
            try {
                //创建一个流套接字并将其连接到指定主机上的指定端口号
                //读取服务器端数据
                DataInputStream input = new DataInputStream(mSocket.getInputStream());
                byte[] buffer;
                buffer = new byte[input.available()];
                if (buffer.length != 0) {
                    // 读取缓冲区
                    input.read(buffer);
                    Intent intent = new Intent();
                    intent.setAction(Utils.ACTION_BROADCAST_IN_ACITIVITY);
                    String res = bytes2hexFirst(buffer);
                    intent.putExtra(Utils.SEND_BROADCAST_FOR_BUFFER,res);
                    sendBroadcast(intent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String bytes2hexFirst(byte[] bytes) {
        final String HEX = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));

        }

        return sb.toString();
    }

    private void connect() {
        final String prefIp = PrefUtils.getString(this, ContastValue.PREF_IP, ContastValue.IP);
        final int prefPort = PrefUtils.getInt(this, ContastValue.PREF_PORT, ContastValue.PORT);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket(InetAddress.getByName(prefIp), prefPort);
                    if (mSocket.isConnected()) {
                        while (true) {
                            try {
                                sleep(500);
                                receiveMsg();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void disconnect() {
        if (mSocket != null) {
            try {
                mSocket.shutdownOutput();
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void send(int cmd, String device) {
        cmdStatic = cmd;
        deviceStatic =device;
        new Thread(){
            @Override
            public void run() {
                super.run();
                if (mSocket != null) {
                    try {
                        OutputStream out = mSocket.getOutputStream();
                        if (out != null) {
                            long currentTime = System.currentTimeMillis()/1000;
                            String ding = String.format("%d%d%d%d", currentTime, currentTime, currentTime, currentTime);
                            String aa = ding.substring(0, 16);
                            byte[] byteDing = aa.getBytes();
                            byte[] time = {(byte) ((currentTime & 0xff000000) >> 24), (byte) ((currentTime & 0x00ff0000) >> 16), (byte) ((currentTime & 0x0000ff00) >> 8), (byte) (currentTime & 0x000000ff)};
                            byte[] deviceId = getDeviceId(deviceStatic);
                            byte[] voice = {(byte) 0x85, 0x00, 0x00, 0x06, deviceId[0], deviceId[1], deviceId[2], deviceId[3], deviceId[4], deviceId[5], 0x00, 0x00, 0x02, 0x1, time[0], time[1], time[2], time[3]};
                            byte[] openLockCmd = {(byte) 0x85, 0x00, 0x00, 0x16, deviceId[0], deviceId[1], deviceId[2], deviceId[3], deviceId[4], deviceId[5], 0x00, 0x00, 0x1, 0x0, time[0], time[1], time[2], time[3]};
                            openLockCmd = addBytes(openLockCmd, byteDing);
                            voice = addBytes(voice, byteDing);
                            if (cmdStatic == 1) {//开锁
                                byte openCode = createCheckCodeReq(openLockCmd);
                                out.write(0x7e);
                                out.write(openLockCmd);
                                out.write(openCode);
                                out.write(0x7e);
                            } else if (cmdStatic == 2) {//语音
                                byte voiceCode = createCheckCodeReq(voice);
                                out.write(0x7e);
                                out.write(voice);
                                out.write(voiceCode);
                                out.write(0x7e);
                            }
                            out.flush();
                            Intent intent = new Intent();
                            intent.setAction(Utils.ACTION_BROADCAST_IN_ACITIVITY);
                            intent.putExtra(Utils.SEND_OK,Utils.SEND_OK_CMD);
                            sendBroadcast(intent);
                            return;
                        } else {
                            if(!mSocket.isConnected()){
                                connect();
                            }
                            Intent intent = new Intent();
                            intent.setAction(Utils.ACTION_BROADCAST_IN_ACITIVITY);
                            intent.putExtra(Utils.ERROR_CONNECT_TOAST,Utils.ERROR_CONNECT_TOAST_CMD);
                            sendBroadcast(intent);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    Intent intent = new Intent();
                    intent.setAction(Utils.ACTION_BROADCAST_IN_ACITIVITY);
                    intent.putExtra(Utils.ERROR_CONNECT_TOAST,Utils.ERROR_CONNECT_TOAST_CMD);
                    sendBroadcast(intent);
                }
            }
        }.start();
    }

    public static byte createCheckCodeReq(byte[] req) {
        byte checkCode = req[0];
        for (int i = 1; i < req.length; i++) {
            checkCode ^= req[i];
        }
        return checkCode;
    }

    public byte[] getDeviceId(String asc) {
        int len = asc.length();
        int mod = len % 2;
        if (mod != 0) {
            asc = "0" + asc;
            len = asc.length();
        }
        byte abt[] = new byte[len];
        if (len >= 2) {
            len = len / 2;
        }
        byte bbt[] = new byte[len];
        abt = asc.getBytes();
        int j, k;
        for (int p = 0; p < asc.length() / 2; p++) {
            if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
                j = abt[2 * p] - '0';
            } else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
                j = abt[2 * p] - 'a' + 0x0a;
            } else {
                j = abt[2 * p] - 'A' + 0x0a;
            }
            if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
                k = abt[2 * p + 1] - '0';
            } else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
                k = abt[2 * p + 1] - 'a' + 0x0a;
            } else {
                k = abt[2 * p + 1] - 'A' + 0x0a;
            }
            int a = (j << 4) + k;
            byte b = (byte) a;
            bbt[p] = b;
        }
        return bbt;
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(acitivtyBroadcast);
        disconnect();
    }

    public class GetMainActivityBroadcast extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null){
                int cmd = intent.getIntExtra("cmd",-1);
                String device = intent.getStringExtra("device");
                if(cmd != -1 && device != null){
                    send(cmd,device);
                }
            }
        }
    }

}
