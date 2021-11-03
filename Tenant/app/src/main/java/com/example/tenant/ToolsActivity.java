package com.example.tenant;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ToolsActivity extends AppCompatActivity {
    String web;
    private String TAG="ToolsActivity";
    public Socket server_socket;
    private InputStream is;
    private OutputStream out;
    public Button commands;
    private String tenant_ID, tenant_pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);
        web=getIntent().getStringExtra("web");
        try {
            new ReceiveThread().start();
        } catch (Exception e) {
            Log.d("Client: ", " Bad!");
            Log.e("Client: ", " Server Connect error "+e.getMessage());
            e.printStackTrace();
        }
        Toast.makeText(this, "Server tcp successful", Toast.LENGTH_SHORT).show();
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
                InetAddress serverIp = InetAddress.getByName("192.168.0.102");
                server_socket = new Socket(serverIp, 9993);
                is=server_socket.getInputStream();
                out=server_socket.getOutputStream();
                Log.d("==========Client for server: ", "start!");
            } catch (Exception e) {
                Log.d("==========Client for server: ", "Bad!");
                Log.e("==========Client for server: ", "Connect error "+e.getMessage());
                e.printStackTrace();
            }

            tenant_ID="phone_tenant";
            tenant_pass="123456";
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recv2();
            send2("ACK");
            recv2();
//          for ACK

            String send_server = "tenant";
            send2(send_server);

            //send my ID pass
            recv2();
            send2(tenant_ID);
            recv2();
            send2(tenant_pass);

            //start use
            recv2();
            send2("command");
            String recv = recv2();
            Log.d("command recv: ", ""+recv);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(recv.equals("OK")){
                send2("get video");
                Log.d("web: ", ""+web);
                Intent intent = new Intent(ToolsActivity.this, StreamActivity.class);
                intent.putExtra("web", web);
                startActivity(intent);
            }
            else{
                Log.d(TAG,"You can't access the video");
                try{
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("server: success", "good!");

        }
    }
}