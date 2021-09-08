package com.example.project2021;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.*;
import java.io.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


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

public class MainActivity extends AppCompatActivity {
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> deviceName;  //定義陣列連接器字串的ID

    Button mBtn_On, mBtn_discover;
    private ListView listView;
    public Socket s1;

    private ConsumerIrManager mCIR;

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

    public void stream(View view) {
        Intent intent = new Intent(MainActivity.this, StreamActivity.class);
        startActivity(intent);
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

//    class ReceiveThread extends Thread {
//
//        private InputStream is;
//        private FileOutputStream fout;
//        private DataInputStream dis;
//        private boolean isConnected;
//
//        public ReceiveThread() {
//            isConnected = true;
//        }
//
//        @Override
//        public void run() {
//            try {
//                InetAddress serverIp = InetAddress.getByName("172.20.10.8");
//                s1 = new Socket(serverIp, 10002);
////                is=s1.getInputStream();
////                fout=s1.getOutputStream();
//                Log.d("Client: ", "start!");
//            } catch (Exception e) {
//                Log.d("Client: ", "Bad!2");
//                Log.e("Client: ", "Connect error "+e.getMessage());
//                e.printStackTrace();
//            }
////            dis = new DataInputStream(is);
////            String temp = new String("hi");
////            try {
////                fout.write(temp.getBytes());
////                fout.flush();
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
//            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test/";
//            String state = Environment.getExternalStorageState();
//            if (!state.equals(Environment.MEDIA_MOUNTED)) {
//                Log.d("Client: ", "canot store !!!");
//                return;
//            }
//            String filename = new String("image");
//
//            //require test filename ok test store image success or faile
//            while (isConnected) {
//                try {
//                    if (fout == null) {
//                        File file = new File(dir+filename+".jpg");
//                        fout = new FileOutputStream(file);
//                    }
//                    is = s1.getInputStream();
//                    dis = new DataInputStream(is);
//                    byte[] buf = new byte[1024];
//                    // int size = is.read(buf);
//                    int size = 163575;
//                    Log.d("Client: ", "size= "+ size);
//                    int len = 0, count_bit = 0, r = size / 1024 + 1, i = 0;
//                    while (true) {
//                        for(long j=0;j<60000l;j++);
//                        i++;
//                        if (i == r) {
//                            len = dis.read(buf, 0, size-count_bit);
//                            Log.d("Client: ", "len= "+len);
//                            fout.write(buf, 0, len);
//                            break;
//                        }
//                        len = dis.read(buf, 0, 1024);
//                        System.out.println("Client: " + len);
//                        fout.write(buf, 0, len);
//                        count_bit += len;
//                        System.out.println(count_bit);
//                    }
//
//                    isConnected = false;
//
//
//                    if (fout != null) {
//                        fout.close();
//                    }
//                    dis.close();
//                    s1.close();
//                } catch (IOException eIO) {
//                    eIO.printStackTrace();
//                }
//
//            }
//
//        }
//
//    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void transmit(View view) {
        if (!mCIR.hasIrEmitter()) {
            Toast.makeText(view.getContext(), "Can not find IR!!!!", Toast.LENGTH_SHORT).show();
        }
        else{
            Log.d("hi", "  ir");
            Toast.makeText(view.getContext(), "IR can use!!!!", Toast.LENGTH_SHORT).show();
//            int[] pattern = {
//                    //directing num
//                    9000,4500,
//
//                    560,565, 560,565, 560,565, 560,1690,
//                    560,565, 560,565, 560,565, 560,565,
//                    560,565, 560,1690, 560,1690, 560,565,
//                    560,565, 560,1690, 560,1690, 560,1690,
//
//                    560,1690, 560,565, 560,565, 560,565, 560,565, 560,565, 560,1690, 560,565,
//
//                    560,565, 560,1690, 560,1690, 560,1690, 560,1690, 560,1690, 560,565, 560,1690,
//                    //end 2 number is ending
//                    560,20000};
            int[] pattern = {
                    //directing num
                    1901, 4453, 625, 1614, 625, 1588, 625, 1614, 625,
                    442, 625, 442, 625, 468, 625, 442, 625, 494, 572, 1614,
                    625, 1588, 625, 1614, 625, 494, 572, 442, 651, 442, 625,
                    442, 625, 442, 625, 1614, 625, 1588, 651, 1588, 625, 442,
                    625, 494, 598, 442, 625, 442, 625, 520, 572, 442, 625, 442,
                    625, 442, 651, 1588, 625, 1614, 625, 1588, 625, 1614, 625,
                    1588, 625, 48958};
//            int k=1000000/38000;
//            for (int i = 0; i < pattern.length; i++){
//                pattern[i] = pattern[i] / k;
//            }
//            for(int i =0;i<pattern.length;i++){
//                Log.d("pattern["+i+"]", " "+pattern[i]);
//            }
            mCIR.transmit(38000, pattern);
            Log.d("can", "  ir");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void check(View view) {
        StringBuilder b = new StringBuilder();
        if (!mCIR.hasIrEmitter()) {
            Toast.makeText(view.getContext(), "Can not find IR!!!!", Toast.LENGTH_SHORT).show();
            return;
        }
        ConsumerIrManager.CarrierFrequencyRange[] freqs = mCIR.getCarrierFrequencies();
        b.append("IR Carrier Frequencies:\n");
        for (ConsumerIrManager.CarrierFrequencyRange range : freqs) {
            b.append(String.format("  %d - %d\n",range.getMinFrequency(), range.getMaxFrequency()));
        }
        Toast.makeText(view.getContext(), b.toString(), Toast.LENGTH_SHORT).show();
    }
    //按下已配對按鈕後作動
    public void test (View view)
    {
        //儲存藍芽設備名稱
        deviceName = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1);
        //儲存藍芽設備
        //取得和本機連接過的設備
        pairedDevices = mBlueAdapter.getBondedDevices();
        //清空陣列連接器字串
        deviceName.clear();
        //如果已配對設備>0
        if (pairedDevices.size() > 0)
        {
            //將pairedDevice資料pass到Device
            for (BluetoothDevice device : pairedDevices)
            {
                //從device取得資料後增加到deviceName內
                Log.v("Nane=" + device.getName(), "Addr=" + device.getAddress());
                scannedDevices.add(new ScannedDevice(device.getName(), device.getAddress()));
                this.deviceName.add(device.getName()+"\n"+device.getAddress());
            }
            myAdapter.notifyDataSetChanged();
        }
        //利用listView顯示已配對藍芽設備
        //listView.setAdapter(deviceName);
    }
}