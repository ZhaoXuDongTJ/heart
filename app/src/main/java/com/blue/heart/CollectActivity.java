package com.blue.heart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class CollectActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice btDevice;
    BluetoothSocket btSocket;

    TextView noticeTV;
    RecyclerView StateTV;
    Button startCollectBtn;
    Button stopCollectBtn;
    Button showCollectBtn;

    StringBuffer buffer2heart2;
    FileOutputStream outputStreams;
    String filePath;

    Thread working = null;
    Thread check2bt = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);

        init();

    }
    public void init(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        noticeTV = (TextView) findViewById(R.id.noticeTV);
        StateTV = (RecyclerView) findViewById(R.id.StateTV);
        startCollectBtn = (Button) findViewById(R.id.startCollectBtn);
        stopCollectBtn = (Button) findViewById(R.id.stopCollectBtn);
        showCollectBtn = (Button) findViewById(R.id.showCollectBtn);

        buffer2heart2 = new StringBuffer();

        IntentFilter filter = new IntentFilter();                                                   // 设置广播信息过滤
        filter.addAction(BluetoothDevice.ACTION_FOUND);                                            //每搜索到一个设备就会发送一个该广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);                            //当全部搜索完后发送该广播
        filter.setPriority(Integer.MAX_VALUE);                                                     //设置优先级
        this.registerReceiver(receiver, filter);                                                   // 注册蓝牙搜索广播接收者，接收并处理搜索结果

        check2bt = new CheckThread();
        check2bt.start();
    }

    class CheckThread extends Thread{
        @Override
        public void run() {
            if(!mBluetoothAdapter.isEnabled()){
                mBluetoothAdapter.enable(); //开启 防止手动关闭
            }
            if(btDevice==null){
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();        //本地找
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if(device.getName().equals(Config.CLIENT_BLUE_NAME)){
                            btDevice = device;
                            break;
                        }
                    }
                }
                if(btDevice==null){                                                               //附件找
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    mBluetoothAdapter.startDiscovery();
                }
                if(btDevice==null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noticeTV.setText(Config.TEXT_MESSAGE+"无设备");
                        }
                    });
                }
            }else {
                try {
                    btSocket =btDevice.createInsecureRfcommSocketToServiceRecord(btDevice.getUuids()[0].getUuid());
                    btSocket.connect();
                    working = new ConnectedThread(btSocket);
                    working.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream input = null;
            OutputStream output = null;
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.inputStream = input;
            this.outputStream = output;
        }

        @Override
        public void run() {
            byte[] buff = new byte[5];
            // 定义每次接收的数据;   一共5个16进制的数据
            int bytes;
            while (true) {
                try {
                    inputStream.read(buff,0,5);
                    buffer2heart2.append(Utils.bin2HexStr(buff)+" ");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                          // re
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 定义广播接收器
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                  //  textView1.append(device.getName() + ":"+ device.getAddress()+"\n");
                    if(device.getName().equals(Config.CLIENT_BLUE_NAME)){
                        btDevice = device;
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //已搜素完成
            }
        }
    };
}
