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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.blue.heart.util.Config;
import com.blue.heart.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static com.blue.heart.R.id.showBlue;
import static com.blue.heart.R.id.showBlueS;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    Button button;
    Button button2;
    Button button3;
    Button connect;
    Button getData;
    Button closeData;
    TextView textView;
    TextView textView1;
    TextView connectText;
    BluetoothAdapter mBluetoothAdapter;
    //
    BluetoothDevice ecgDevice;
    BluetoothSocket btSocket;
    //接收数据
    String string;
    byte[] datas;
    StringBuffer buffer;
    FileOutputStream outputStreams;
    //
    Thread tt = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        button = (Button) findViewById(R.id.openBlue);
        button2 = (Button)findViewById(R.id.searchBlue);
        button3 = (Button)findViewById(R.id.searchBlueS);
        getData = (Button)findViewById(R.id.getData);
        connect = (Button)findViewById(R.id.connect);
        closeData = (Button)findViewById(R.id.closeData);
        textView = (TextView) findViewById(showBlue);
        textView1 = (TextView) findViewById(showBlueS);
        connectText = (TextView) findViewById(R.id.connectText);

        buffer = new StringBuffer();
        String ss = Utils.getFilePath();
        Log.d("MainActivity",ss+"---------------------------------------------------------");

        File file = new File(ss);
        Toast.makeText(MainActivity.this,ss+"",Toast.LENGTH_SHORT).show();

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
        try {
            outputStreams = new FileOutputStream(file,true);
            Log.d("MainActivity","fileoutputstring"+"---------------------------------------------------------");
        } catch (FileNotFoundException e) {
            Toast.makeText(MainActivity.this,"ERROR outputStreams",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            Log.d("MainActivity",e.getMessage()+"---------------------------------------------------------");
        }


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mBluetoothAdapter.isEnabled()){
                    mBluetoothAdapter.enable(); //开启
                }else {
                    mBluetoothAdapter.disable();//关闭
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        textView.append(device.getName() + ":" + device.getAddress()+"\n");
                        if(device.getName().equals(Config.CLIENT_BLUE_NAME)){
                            Toast.makeText(MainActivity.this,"找到设备",Toast.LENGTH_SHORT).show();
                            ecgDevice = device;
                        }
                    }
                }
            }
        });

        IntentFilter filter = new IntentFilter();                                                   // 设置广播信息过滤
        filter.addAction(BluetoothDevice.ACTION_FOUND);                                            //每搜索到一个设备就会发送一个该广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);                            //当全部搜索完后发送该广播
        filter.setPriority(Integer.MAX_VALUE);                                                     //设置优先级
        this.registerReceiver(receiver, filter);                                                   // 注册蓝牙搜索广播接收者，接收并处理搜索结果

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //如果当前在搜索，就先取消搜索
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                //开启搜索
                mBluetoothAdapter.startDiscovery();
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UUID uuid = ecgDevice.getUuids()[0].getUuid();
                textView1.setText(uuid.toString());

                try {
                    btSocket = ecgDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    btSocket.connect();
                    Toast.makeText(MainActivity.this,"OK",Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"Connect Error",Toast.LENGTH_SHORT).show();
                }
            }
        });
        getData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tt == null){
                    tt = new ConnectedThread(btSocket);
                    tt.start();
                }
                Toast.makeText(MainActivity.this,"触发",Toast.LENGTH_SHORT).show();
            }
        });
        closeData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tt!=null){
                    tt.interrupt();
                }
                Toast.makeText(MainActivity.this,"停止采集数据",Toast.LENGTH_SHORT).show();
                try {
                    outputStreams.write(new String(buffer).getBytes());
                    outputStreams.flush();
                    outputStreams.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

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
                    textView1.append(device.getName() + ":"+ device.getAddress()+"\n");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //已搜素完成
            }
        }
    };

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
                    string = Utils.bin2HexStr(buff);
                    buffer.append(string+" ");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(tt!=null){
                                connectText.setText(string);
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

}
