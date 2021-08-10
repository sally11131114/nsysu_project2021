package com.example.project2021;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    Button mBtn_On, mBtn_discover;
    private ListView listView;

    MyAdapter myAdapter;
    BluetoothAdapter mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList<ScannedDevice> scannedDevices = new ArrayList<ScannedDevice>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                startActivity(intent);
            }
        });
    }

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
}