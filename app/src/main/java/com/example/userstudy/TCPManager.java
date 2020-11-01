package com.example.userstudy;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by wengturtle on 2019/3/29.
 */


interface INetworkCallback {
    void onReceiveData(String data);
}

public class TCPManager {

    /** TCP Network */
    private String IP;
    private int PORT;
    private Socket mSocket;

    /** IO Stream */
    private InputStream is;
    private InputStreamReader isr;
    private BufferedReader br;
    private OutputStream os;

    public INetworkCallback callback;

    public TCPManager(String ip, int port) {
        this.IP = ip;
        this.PORT = port;
    }

    public void start() {
        new ConnectThread().start();
    }

    private class ConnectThread extends Thread {

        @Override
        public void run() {
            super.run();

            try{
                mSocket = new Socket(IP,PORT);
                new recvThread().start();
                Log.i("YueTing", "test");
                Log.d("YueTing", String.valueOf(mSocket.isConnected()) + "from server");

            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private class recvThread extends Thread {
        @Override
        public void run() {
            super.run();

            while(mSocket != null){

                try{

                    is = mSocket.getInputStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);

                    String response = br.readLine();

                    if(response == null) { continue; }

                    Log.d("YueTing",response);
                    callback.onReceiveData(response);

                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private class SendThread extends Thread {
        @Override
        public void run() {
            super.run();

            long prev = System.currentTimeMillis();
            long cur = prev;
            while(mSocket.isConnected()){

                cur = System.currentTimeMillis();

                if((cur - prev) * 1000 <= 3.0) {
                    prev = cur;
                    send("alive");
                }
            }
        }
    }

    public void send(final byte[] bytes)
    {
        if(mSocket == null) { return; }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 步骤1：从Socket 获得输出流对象OutputStream
                    os = mSocket.getOutputStream();

                    int n = bytes.length;

                    // 步骤2：写入需要发送的数据到输出流对象中
                    os.write(String.format("%7d\n", n).getBytes());
                    os.write(bytes);
                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                    // 步骤3：发送数据到服务端
                    os.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void send(final Bitmap bitmap) {

        if(mSocket == null) { return; }

        // 利用线程池直接开启一个线程 & 执行该线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    final OutputStream out = new DataOutputStream(mSocket.getOutputStream());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    //读取图片到ByteArrayOutputStream
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos); //hold
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); //finger

                    Log.d("ImageSize",String.valueOf(bitmap.getWidth()));
                    Log.d("ImageSize",String.valueOf(bitmap.getHeight()));

                    byte[] bytes = baos.toByteArray();

                    Log.d("YueTing ByteLength",String.valueOf(bytes.length));

                    out.write(String.format("%4d", bytes.length).getBytes());
                    out.write(bytes);
                    out.flush();
                    baos.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void send(String msg) {

        if(mSocket == null) { return; }

        final String str = msg;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 步骤1：从Socket 获得输出流对象OutputStream
                    os = mSocket.getOutputStream();

                    // 步骤2：写入需要发送的数据到输出流对象中
                    os.write((str + "\n" ).getBytes("utf-8"));
                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                    // 步骤3：发送数据到服务端
                    os.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}