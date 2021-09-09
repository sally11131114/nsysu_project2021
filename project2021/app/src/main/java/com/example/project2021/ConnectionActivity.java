package com.example.project2021;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

public class ConnectionActivity extends AppCompatActivity implements ExampleDialog.ExampleDialogListener{
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
    String Router_name, Router_pass;
    private String iot_public_key_string;
    private String my_public_key_string, my_private_key_string;
    private RSAPublicKey iot_public_key;
    private RSAPublicKey my_public_key;
    private RSAPrivateKey my_private_key;

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

    @Override
    public void applyTexts(String username, String password) {
        Router_name = username;
        Router_pass = password;
        Log.v("ID:"+Router_name, " PASS:"+Router_pass);
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
        ConnectedThread.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            communication_beforeIR();
        }
//
//        int IR_check = transmit();
//        if(IR_check==0){
//            Log.v("IR_check", "failed");
//            return;
//        }
//        Log.v("IR_check", "successful");
//
//        ExampleDialog Dialog = new ExampleDialog();
//        Dialog.show(getSupportFragmentManager(), "example dialog");
//        String web = communication_afterIR();
//
//
//        Intent intent = new Intent(ConnectionActivity.this, StreamActivity.class);
//        intent.putExtra("web", web);
//        startActivity(intent);
    }

    public void disconnect(View view) {
        if(mSocket==null){
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void communication_beforeIR(){
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
        //for ACK

        String temp2_send = new String("HTC HTC One");
        send(temp2_send);
        Log.d(TAG, "Send the message"+temp2_send);


        try {
            count = mInputstream.read(buffer);
            String temp = new String(buffer, 0, count);
            iot_public_key_string= temp;
            Log.d(TAG, "InputStream:" + temp);
        } catch (IOException e) {
            Log.e(TAG, "Error inputstream. " + e.getMessage());
        }
        //receive iot public key

        send_key();

//        try {
//            count = mInputstream.read(buffer);
//            ir_recv = new String(buffer, 0, count);
//            Log.d(TAG, "InputStream:" + ir_recv);
//            Log.d(TAG, "InputStream.len:" + ir_recv.length());
//
//        } catch (IOException e) {
//            Log.e(TAG, "Error inputstrea " + e.getMessage());
//        }
        //for ACK

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void send_key(){
        try{
            //turn iot_key_string into publickey
            KeyFactory keyFactory = KeyFactory.getInstance("RSA"); //NoSuchAlgorithmException
            Base64.Decoder iot_decoder = Base64.getDecoder();
            byte[] keyBytes = iot_decoder.decode(iot_public_key_string);
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
            iot_public_key = (RSAPublicKey)keyFactory.generatePublic(x509EncodedKeySpec); //InvalidKeySepcException
        }catch (Exception e) {
            e.printStackTrace();
        }

        //creat my key
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (Exception e) {
            e.printStackTrace();
        }
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        my_public_key = (RSAPublicKey)keyPair.getPublic();
        my_private_key = (RSAPrivateKey)keyPair.getPrivate();
        Base64.Encoder my_encoder = Base64.getEncoder();
        my_public_key_string = my_encoder.encodeToString(my_public_key.getEncoded());
        send(my_public_key_string);
        Log.d(TAG, "Send the message"+my_public_key_string);

    }

    public String communication_afterIR(){
        send(Router_name);
        Log.d(TAG, "Send the Router_name"+Router_name);

        send(Router_pass);
        Log.d(TAG, "Send the Router_pass"+Router_name);

        String web = null;
        byte[] buffer = new byte[2048];
        int count;
        try {
            count = mInputstream.read(buffer);
            String temp = new String(buffer, 0, count);
            Log.d(TAG, "InputStream:" + temp);
            web = temp;
        } catch (IOException e) {
            Log.e(TAG, "Error inputstream. " + e.getMessage());
        }
//        for ACK
        return web;
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
    public int transmit() {
        int check=0;
        if (!mCIR.hasIrEmitter()) {
            Toast.makeText(this, "Can not find IR!!!!", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "IR can use!!!!", Toast.LENGTH_SHORT).show();
//
//            int sum=0;
//            for(int i=0;i<ir_recv.length();i++){
//                int temp = ir_recv.charAt(i);
//                sum+=temp-65;
//            }
//            Log.d(TAG, "sum: "+sum);
//            String ir_control = Integer.toBinaryString(sum);
//            Log.d(TAG, "binary sum: "+ir_control);
//
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

            int count;
            byte[] buffer = new byte[2048];
            try {
                count = mInputstream.read(buffer);
                String temp = new String(buffer, 0, count);
                Log.d(TAG, "InputStream:" + temp);
                if(temp=="verification successful"){
                    check = 1;
                }
                else{
                    check =0;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error inputstrea " + e.getMessage());
            }
        }

        return check;
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

    public void send_router(View view) {
        ExampleDialog Dialog = new ExampleDialog();
        Dialog.show(getSupportFragmentManager(), "example dialog");
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