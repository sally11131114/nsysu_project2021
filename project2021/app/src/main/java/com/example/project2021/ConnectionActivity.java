package com.example.project2021;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
    private ConsumerIrManager mCIR;

    TextView tv1;
    String ir_recv;


//    private Thread ConnectedThread = new Thread(new Runnable(){
//        public void run(){
//            byte[] buffer = new byte[2048];
//            int count;
//            if (mSocket!=null) {
//                while (true) {
//                    try {
//                        count = mInputstream.read(buffer);
//                        String temp = new String(buffer, 0, count);
//                        Log.d(TAG, "InputStream:" + temp);
//
//                    } catch (IOException e) {
//                        Log.e(TAG, "Error inputstream. " + e.getMessage());
//                    }
//                }
//            }
//        }
//    });

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        tv1 = findViewById(R.id.device);
        device_name=getIntent().getStringExtra("DeviceName");
        device_addr=getIntent().getStringExtra("DeviceAddr");
        tv1.setText("Device: " + device_name + "\n\t" + device_addr);
        mCIR = (ConsumerIrManager)this.getSystemService(Context.CONSUMER_IR_SERVICE);

   }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void connect(View view) throws IOException {
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
//        ConnectedThread.start();
        byte[] buffer = new byte[2048];
        int count;

        String temp_send = new String("Hello IOTserver.");
        send(temp_send);
        Log.d(TAG, "Send the message"+temp_send);

        try {
            count = mInputstream.read(buffer);
            String temp = new String(buffer, 0, count);
            Log.d(TAG, "InputStream:" + temp);

        } catch (IOException e) {
            Log.e(TAG, "Error inputstream. " + e.getMessage());
        }

        String temp2_send = new String("HTC HTC One");
        send(temp2_send);
        Log.d(TAG, "Send the message"+temp2_send);

        try {
            count = mInputstream.read(buffer);
            ir_recv = new String(buffer, 0, count);
            Log.d(TAG, "InputStream:" + ir_recv);
            Log.d(TAG, "InputStream.len:" + ir_recv.length());

        } catch (IOException e) {
            Log.e(TAG, "Error inputstrea " + e.getMessage());
        }

        transmit();
    }

    public void disconnect(View view) {
        if(mSocket==null){

            //test
//            int sum= 31;
//            String ir_control = Integer.toBinaryString(sum);
//            Log.d("test:", "binary sum: "+ir_control);
//            Log.d("test:", "binary sum.length: "+ir_control.length());
//            int[] pattern ={9000, 4500};
//            for(int i=0;i<ir_control.length();i++){
//                if(ir_control.charAt(i)=='1'){
//                    Log.d("i:"+i, "ir_control.charAt(i):"+ir_control.charAt(i));
//                    pattern = add_int(pattern.length, pattern, 1);
//                }
//                else if(ir_control.charAt(i)=='0'){
//                    Log.d("i:"+i, "ir_control.charAt(i):"+ir_control.charAt(i));
//                    pattern = add_int(pattern.length, pattern, 0);
//                }
//            }
//            pattern = add_int(pattern.length, pattern, 2);
//            for(int i =0;i<pattern.length;i++){
//                Log.d("pattern["+i+"]", " "+pattern[i]);
//            }
            //test
            return;
        }
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void transmit() {
        if (!mCIR.hasIrEmitter()) {
            Toast.makeText(this, "Can not find IR!!!!", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "IR can use!!!!", Toast.LENGTH_SHORT).show();

            int sum=0;
            for(int i=0;i<ir_recv.length();i++){
                int temp = ir_recv.charAt(i);
                sum+=temp-65;
            }
            Log.d(TAG, "sum: "+sum);
            String ir_control = Integer.toBinaryString(sum);
            Log.d(TAG, "binary sum: "+ir_control);

//            int[] pattern ={9000, 4500};
//            for(int i=0;i<ir_control.length();i++){
//               if(ir_control.charAt(i)=='1'){
//                    pattern = add_int(pattern.length, pattern, 1);
//                }
//                else if(ir_control.charAt(i)=='0'){
//                    pattern = add_int(pattern.length, pattern, 0);
//                }
//            }
//            pattern = add_int(pattern.length, pattern, 2);
            int[] pattern = {
                    //directing num
                    9000,4500,

                    560,565, 560,565 ,560,1690, 560,1690, // 3
                    //end 2 number is ending
                    560,20000};

            mCIR.transmit(38000, pattern);
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

    public static int[] add_int(int n, int arr[], int x){
        int newarr[] = new int[n+2];

        for(int i=0;i<n;i++)
            newarr[i]=arr[i];
        newarr[n]=560;
        if(x==1){
            newarr[n+1]=1690;
        }
        else if(x==0){
            newarr[n+1]=565;
        }
        else{
            newarr[n+1]=20000;
        }
        return newarr;
    }

}