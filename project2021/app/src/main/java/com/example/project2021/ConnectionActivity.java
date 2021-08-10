package com.example.project2021;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

public class ConnectionActivity extends AppCompatActivity {
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String TAG="ConnectionActivity";
    private String device_name, device_addr;
    private BluetoothSocket mSocket;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private InputStream mInputstream;
    private OutputStream mOutputstream;

    TextView tv1;


    private Thread ConnectedThread = new Thread(new Runnable(){
        public void run(){
            byte[] buffer = new byte[2048];
            int count;
            if (mSocket!=null) {
                while (true) {
                    try {
                        count = mInputstream.read(buffer);
                        String temp = new String(buffer, 0, count);
                        Log.d(TAG, "InputStream:" + temp);
                    } catch (IOException e) {
                        Log.e(TAG, "Error inputstream. " + e.getMessage());
                    }
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        tv1 = findViewById(R.id.device);
        device_name=getIntent().getStringExtra("DeviceName");
        device_addr=getIntent().getStringExtra("DeviceAddr");
        tv1.setText("Device: " + device_name + "\n\t" + device_addr);
   }

    public void connect(View view) {
        BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(device_addr);
        try{
            Log.d(TAG, "Try to creat socket...");
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mInputstream = mSocket.getInputStream();
            mOutputstream = mSocket.getOutputStream();
        }catch(IOException e){
            Log.e(TAG, "Connect error."+e.getMessage());
        }
        mBluetoothAdapter.cancelDiscovery();

        try{
            Log.d(TAG, "Try to connect...");
            mSocket.connect();
        }catch (IOException e){
            try {
                mSocket.close();
                Log.d(TAG, "Closed socket.");
            }catch (IOException e2){
                Log.e(TAG, "Can not close socket."+e2.getMessage());
            }

            Log.e(TAG, "Can not connect to uuid"+e.getMessage());
        }
        ConnectedThread.start();
        String temp_send = new String("Hello IOTserver.");
        send(temp_send);
        Log.d(TAG, "Send the message"+temp_send);
        String temp2_send = new String("Bad IOTserver.");
        send(temp2_send);
        Log.d(TAG, "Send the message"+temp2_send);
        String temp3_send = new String("Bad IOTserver.");
        send(temp3_send);
        Log.d(TAG, "Send the message"+temp3_send);
    }

    public void disconnect(View view) {
        if(mSocket==null){ return;}
        try{
            mSocket.close();
            mSocket = null;
            mInputstream = null;
            mOutputstream = null;
        }catch(IOException e){
            Log.e(TAG, "Disconnect error."+e.getMessage());
        }
    }

    public void send(String output){
        if(output == null){
            return;
        }
        try{
            mOutputstream.write(output.getBytes());
            mOutputstream.flush();
            Log.d(TAG, "Outputstream: "+output);
        }catch (IOException e){
            Log.e(TAG, "Send error."+e.getMessage());
        }
    }

}