package com.example.tenant;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class TenantConnectionActivity extends AppCompatActivity implements ExampleDialog.ExampleDialogListener{
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    TextView tv1, tv2;
    private String TAG="TenantConnectionActivity";
    private String device_name, device_addr;
    private ConsumerIrManager mCIR;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket mSocket;
    private InputStream mInputstream;
    private OutputStream mOutputstream;

    private InputStream is;
    private OutputStream out;
    String tenant_ID, tenant_pass;
    public Socket server_socket;
    private String iot_public_key_string;
    private String my_public_key_string, my_private_key_string;
    private RSAPublicKey iot_public_key;
    private RSAPublicKey my_public_key;
    private RSAPrivateKey my_private_key;
    private String web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_connection);
        tv1 = findViewById(R.id.device);
        tv2 = findViewById(R.id.phase);
        device_name=getIntent().getStringExtra("DeviceName");
        device_addr=getIntent().getStringExtra("DeviceAddr");
        tv1.setText("Device: " + device_name + "\n\t" + device_addr);
        mCIR = (ConsumerIrManager)this.getSystemService(Context.CONSUMER_IR_SERVICE);
        Log.v("HI", "connect");
    }
    @Override
    public void applyTexts(String username, String password) {
        tenant_ID = username;
        tenant_pass = password;
        Log.v("ID:"+tenant_ID, " PASS:"+tenant_pass);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void connect(View view) throws InterruptedException {
        BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(device_addr);
        try{
            Log.d(TAG, "Try to creat socket...");
            tv2.setText("State:\nBluetooth connecting...");
            tv2.postInvalidate();
            Thread.sleep(1000);
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
        after_connect();
        Thread.sleep(500);
        int IR_check = transmit();
        int i=0;
        if(IR_check==0){
            Log.v("IR_check", "failed");
            i++;
        }
        else{
            Log.v("IR_check", "successful");
            Log.v("Router", "transmit");
        }
        while(IR_check == 0){
            Thread.sleep(500);
            IR_check = transmit();
            Log.v("IR_check", ""+IR_check);
            if(i==10){
                break;
            }
            if(IR_check==0){
                Log.v("IR_check", "failed");
                i++;
            }
            else{
                Log.v("IR_check", "successful");
                Log.v("Router", "transmit");
                break;
            }
        }
        tv2.setText("State:\nClick button to connect with server");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void after_connect() throws InterruptedException {
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

        String temp2_send = new String("Tenant");
        send(temp2_send);
        Log.d(TAG, "Send the message"+temp2_send);


        try {
            count = mInputstream.read(buffer);
            String temp = new String(buffer, 0, count);
            iot_public_key_string= temp;
            Log.d(TAG, "receive iot public key:" + temp);
            Log.d(TAG, "receive iot public key.len:" + temp.length());
        } catch (IOException e) {
            Log.e(TAG, "Error inputstream. " + e.getMessage());
        }
        //receive iot public key

        send_key();
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

    public int transmit() throws InterruptedException {
        int check=0;
        tv2.setText("State:\nIR verifying...");
        Thread.sleep(1000);
        tv2.postInvalidate();
        if (!mCIR.hasIrEmitter()) {
            Toast.makeText(this, "Can not find IR!!!!", Toast.LENGTH_SHORT).show();
        }
        else{
//            Toast.makeText(this, "IR can use!!!!", Toast.LENGTH_SHORT).show();

            int sum=0;
            for(int i=0;i<=63;i++){
                int temp = iot_public_key_string.charAt(i);
                Log.d(TAG, "temp: "+temp);
                Log.d(TAG, "char: "+iot_public_key_string.charAt(i));
                sum+=temp*23456;
            }

            Log.d(TAG, "sum: "+sum);
            String ir_control = Integer.toBinaryString(sum);
            Log.d(TAG, "binary sum: "+ir_control);
            int[] pattern ={9000, 4500};
            for(int i=0;i<ir_control.length();i++){
                if(ir_control.charAt(i)=='1'){
                    pattern = add_int(pattern.length, pattern, 1);
                }
                else if(ir_control.charAt(i)=='0'){
                    pattern = add_int(pattern.length, pattern, 0);
                }
            }
            pattern = add_int(pattern.length, pattern, 2);

            mCIR.transmit(38000, pattern);

            int count;
            byte[] buffer = new byte[2048];
            try {
                count = mInputstream.read(buffer);
                String temp = new String(buffer, 0, count);
                Log.d(TAG, "InputStream:" + temp);
                int test = temp.charAt(0);
                Log.d(TAG, "InputStream:" + test);
                if(test=='v'){
                    check = 1;
                    Log.d(TAG, "HI:" + "suc");
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

    public void set_stream(View view) throws IOException {
        send("ACK");
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
//        for web
        mSocket.close();
        Intent intent = new Intent(TenantConnectionActivity.this, ToolsActivity.class);
        intent.putExtra("web", web);
        startActivity(intent);
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

    public String recv(){
        String s = null;
        byte[] buffer = new byte[2048];
        int count;
        try {
            count = mInputstream.read(buffer);
            String temp = new String(buffer, 0, count);
            s=temp;
            Log.d(TAG, "==========InputStream:" + temp);
        } catch (IOException e) {
            Log.e(TAG, "==========Error inputstream. " + e.getMessage());
        }
        return s;
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