package com.example.project2021;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.*;
import java.io.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.ConsumerIrManager;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class MainActivity extends AppCompatActivity{
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> deviceName;  //定義陣列連接器字串的ID
    public String TAG = "main login";
    Button mBtn_On, mBtn_discover;
    private ListView listView;
    public Socket s1;
    GlobalVariable login_socket;
    public Socket server_socket;
    private ConsumerIrManager mCIR;
    String owner_ID, owner_pass;
    private InputStream is;
    private OutputStream out;

    MyAdapter myAdapter;
    BluetoothAdapter mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList<ScannedDevice> scannedDevices = new ArrayList<ScannedDevice>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("Hello", "broadcast");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName() != null) {
                    Log.v("Nane=" + device.getName(), "Addr=" + device.getAddress());
                    scannedDevices.add(new ScannedDevice(device.getName(), device.getAddress()));
                    myAdapter.notifyDataSetChanged();
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals((action))){
                if(mBlueAdapter.isDiscovering()){
                    mBlueAdapter.cancelDiscovery();
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        owner_ID="phone_owner";
        owner_pass="123456";
        mBtn_On         = findViewById(R.id.btn_on);
        listView        = findViewById(R.id.lv);
        //initialization
        myAdapter = new MyAdapter(this, scannedDevices);
        listView.setAdapter(myAdapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("You touch the Nane=" + myAdapter.getItem(position).getName(), "Addr=" + myAdapter.getItem(position).getAddr());
                if (mBlueAdapter.isDiscovering()){
                    mBlueAdapter.cancelDiscovery();
                }
                Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
                intent.putExtra("DeviceName", myAdapter.getItem(position).getName());
                intent.putExtra("DeviceAddr", myAdapter.getItem(position).getAddr());
                intent.putExtra("owner_ID", owner_ID);
                intent.putExtra("owner_pass", owner_pass);
                startActivity(intent);
            }
        });
        mCIR = (ConsumerIrManager)this.getSystemService(Context.CONSUMER_IR_SERVICE);
    }
//    public void tcp_test(View view) {
//        try {
//            new ReceiveThread().start();
//        } catch (Exception e) {
//            Log.d("Client: ", "Bad!1");
//            Log.e("Client: ", "Connect error "+e.getMessage());
//            e.printStackTrace();
//        }
//    }


    public void TurnOn(View v) {
        if(mBlueAdapter == null ){
            Toast.makeText(v.getContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
        }
        else {
            //open bluetooth
            if (!mBlueAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 0);
            }
            else {
                Toast.makeText(v.getContext(), "Bluetooth is already on.", Toast.LENGTH_SHORT).show();
                //////tcp server
                try {
                    new ReceiveThread().start();
                } catch (Exception e) {
                    Log.d("Client: ", " Bad!");
                    Log.e("Client: ", " Server Connect error "+e.getMessage());
                    e.printStackTrace();
                }
                Toast.makeText(this, "Server tcp successful", Toast.LENGTH_SHORT).show();
                //////tcp server
            }
        }

    }

    public void StartScan(View v) {
        if (mBlueAdapter.isDiscovering()){
            mBlueAdapter.cancelDiscovery();
        }
        scannedDevices.clear();
        //re-start discovery
        mBlueAdapter.startDiscovery();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        Toast.makeText(v.getContext(), "Start scan...", Toast.LENGTH_SHORT).show();
        Log.d("lt: ", "Start scan...");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0){
            switch (resultCode){
                case RESULT_OK:
                    Toast.makeText(this, "Turn on successfully.", Toast.LENGTH_SHORT).show();
                    //藍芽開啟成功
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, "Failed to turn on bluetooth", Toast.LENGTH_SHORT).show();
                    //藍芽開啟失敗
                    break;
            }
        }
    }

    class MyAdapter extends ArrayAdapter<ScannedDevice> {
        MyAdapter (Context ct, ArrayList<ScannedDevice> scannedDevices){
            super(ct, 0, scannedDevices);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View item = convertView;
            if(item == null){
                item = LayoutInflater.from(getContext()).inflate(R.layout.item, parent, false);
            }

            ScannedDevice current = getItem(position);
            TextView myname = item.findViewById(R.id.item_tv1);
            TextView myaddr = item.findViewById(R.id.item_tv2);

            myname.setText(current.getName());
            myaddr.setText(current.getAddr());

            return item;
        }

        @Nullable
        @Override
        public ScannedDevice getItem(int position) {
            return super.getItem(position);
        }
    }

    static class ScannedDevice {
        private String name;
        private String addr;

        ScannedDevice(String n, String a){
            name = n;
            addr = a;
        }
        String getName(){
            return name;
        }
        String getAddr(){
            return addr;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mBlueAdapter.cancelDiscovery();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void test (View view)
    {
        deviceName = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1);
        pairedDevices = mBlueAdapter.getBondedDevices();
        deviceName.clear();
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                Log.v("Nane=" + device.getName(), "Addr=" + device.getAddress());
                scannedDevices.add(new ScannedDevice(device.getName(), device.getAddress()));
                this.deviceName.add(device.getName()+"\n"+device.getAddress());
            }
            myAdapter.notifyDataSetChanged();
        }
    }

    public void send2(String output){
        if(output == null){
            return;
        }
        try{
            out.write(output.getBytes());
            out.flush();
            Log.d(TAG, "Outputstream: "+output);
        }catch (IOException e){
            Log.e(TAG, "Send error."+e.getMessage());
        }
    }

    public String recv2(){
        String s = null;
        byte[] buffer = new byte[2048];
        int count;
        try {
            count = is.read(buffer);
            String temp = new String(buffer, 0, count);
            s=temp;
            Log.d(TAG, "==========InputStream:" + temp);
        } catch (IOException e) {
            Log.e(TAG, "==========Error inputstream. " + e.getMessage());
        }
        return s;
    }
    class ReceiveThread extends Thread {

        private boolean isConnected;
        private String command_name;
        private int test;

        public ReceiveThread() {
            is = null;
            out = null;
            isConnected = true;
            command_name = null;
            test = 0;
        }

        @SuppressLint("LongLogTag")
        @Override
        public void run() {
            try {
                InetAddress serverIp = InetAddress.getByName("192.168.0.115");
                server_socket = new Socket(serverIp, 9994);
                is=server_socket.getInputStream();
                out=server_socket.getOutputStream();
                Log.d("==========Client for server: ", "start!");
            } catch (Exception e) {
                Log.d("==========Client for server: ", "Bad!");
                Log.e("==========Client for server: ", "Connect error "+e.getMessage());
                e.printStackTrace();
            }

            owner_ID="phone_owner";
            owner_pass="123456";
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recv2();
            send2("ACK");
            recv2();
//          for ACK

            String send_server = "owner";
            send2(send_server);

            recv2();
            send2("1");

            //send my ID pass
            recv2();
            send2(owner_ID);
            recv2();
            send2(owner_pass);
            recv2();
            //start use
            Log.d("server: success", "good!");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            try {
//                server_socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        }

    }
}