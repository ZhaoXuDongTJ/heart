package com.blue.heart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.blue.heart.util.ClsUtils;
import com.blue.heart.util.Config;
import com.blue.heart.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class CollectActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice btDevice;
    BluetoothSocket btSocket;
    boolean isDevice =false;

    TextView noticeTV;
    TextView StateTV;
    Button startCollectBtn;
    Button stopCollectBtn;
    Button showCollectBtn;

    StringBuffer buffer2heart2;
    String se;
    FileOutputStream outputStreams;
    String filePath;

    Thread working = null;
    Thread check2bt = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);

        init();

        startCollectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buffer2heart2==null){
                    buffer2heart2 = new StringBuffer();
                }
                try {
                    if(btSocket==null){
                        btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(btDevice.getUuids()[0].getUuid());
                        btSocket.connect();
                        if(btSocket.isConnected()){
                            noticeTV.setText(Config.TEXT_MESSAGE+"设备已经连接");
                            stopCollectBtn.setEnabled(true);
                            startCollectBtn.setEnabled(false);
                            working = new ConnectedThread(btSocket);
                            working.start();
                        }else {
                            StateTV.setText("连接失败");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        stopCollectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(working!=null){
                    working.interrupt();
                }
                startCollectBtn.setEnabled(true);
                stopCollectBtn.setEnabled(false);
                showCollectBtn.setEnabled(true);
                new SaveFile().start();
            }
        });
        showCollectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(filePath), "text/plain");
                startActivity(intent);
            }
        });
    }
    public void init(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        noticeTV = (TextView) findViewById(R.id.noticeTV);
        StateTV = (TextView) findViewById(R.id.StateTV);
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
            while (true){
                if(!mBluetoothAdapter.isEnabled()){
                    mBluetoothAdapter.enable();
                }
                if(btDevice==null){
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();        //本地找
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            if(device.getName().equals(Config.CLIENT_BLUE_NAME)){
                                btDevice = device;
                                isDevice = true;
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
                    }else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startCollectBtn.setEnabled(true);
                                noticeTV.setText(Config.TEXT_MESSAGE+"设备发现");
                            }
                        });
                    }
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startCollectBtn.setEnabled(true);
                            noticeTV.setText(Config.TEXT_MESSAGE+"设备发现");
                        }
                    });
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
            byte[] buff = new byte[5*10];                            // 定义每次接收的数据;   一共5个16进制的数据
            while (true) {
                try {
                    inputStream.read(buff,0,5*10);
                    se = Utils.bin2HexStr(buff);
                    buffer2heart2.append(se+" ");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(se.length()>0&&se!=null){
                                StateTV.setText(se);
                            }
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

    class SaveFile extends Thread{
        @Override
        public void run() {
            synchronized (this){
                filePath = Utils.getFilePath();
                File file = new File(filePath);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(CollectActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                try {
                    outputStreams = new FileOutputStream(file,true);
                    outputStreams.write(new String(buffer2heart2).getBytes());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noticeTV.setText(Config.TEXT_MESSAGE+"保存");
                        }
                    });
                    outputStreams.flush();
                    outputStreams.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(CollectActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                    if(device.getName().equals(Config.CLIENT_BLUE_NAME)){
                        try {
                            ClsUtils.setPin(device.getClass(), device, "1234");//设置pin值
                            ClsUtils.createBond(device.getClass(), device);
                            btDevice = device;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //已搜素完成
            }
        }
    };
}
