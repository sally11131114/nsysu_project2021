package com.example.tenant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private String TAG="Tenant";
    private ConsumerIrManager mCIR;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> deviceName;  //定義陣列連接器字串的ID
    MyAdapter myAdapter;
    BluetoothAdapter mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList<ScannedDevice> scannedDevices = new ArrayList<ScannedDevice>();
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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
                Intent intent = new Intent(MainActivity.this, TenantConnectionActivity.class);
                intent.putExtra("DeviceName", myAdapter.getItem(position).getName());
                intent.putExtra("DeviceAddr", myAdapter.getItem(position).getAddr());
                startActivity(intent);
            }
        });
        mCIR = (ConsumerIrManager)this.getSystemService(Context.CONSUMER_IR_SERVICE);
    }

    public void send_ID_pass(View view) {
        ExampleDialog Dialog = new ExampleDialog();
        Dialog.show(getSupportFragmentManager(), "example dialog");
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



}