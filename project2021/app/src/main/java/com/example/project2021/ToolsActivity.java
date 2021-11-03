package com.example.project2021;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ToolsActivity extends AppCompatActivity implements ExampleDialog.ExampleDialogListener{
    String web;
    private String TAG="ToolsActivity";
    public Socket server_socket;
    private InputStream is;
    private OutputStream out;
    public EditText et_name;
    public Button btn_Show, addt, commands, login;


    String owner_ID, owner_pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);
        owner_ID=getIntent().getStringExtra("owner_ID");
        owner_pass=getIntent().getStringExtra("owner_pass");
        web=getIntent().getStringExtra("web");
        login = findViewById(R.id.login);
        login.setVisibility(View.VISIBLE);
        try {
            new ReceiveThread().start();
        } catch (Exception e) {
            Log.d("Client: ", " Bad!");
            Log.e("Client: ", " Server Connect error "+e.getMessage());
            e.printStackTrace();
        }
    }
    public void applyTexts(String username, String password) throws InterruptedException {
        String owner_ID = username;
        String owner_pass = password;
        Log.v("ID:"+owner_ID, " PASS:"+owner_pass);
        addt.setVisibility(View.VISIBLE);
        commands.setVisibility(View.VISIBLE);
        login.setVisibility(View.GONE);
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

    public void login(View view) throws InterruptedException {
        ExampleDialog Dialog = new ExampleDialog();
        Dialog.show(getSupportFragmentManager(), "example dialog");
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

            addt = findViewById(R.id.addt);
            commands = findViewById(R.id.commands);

            addt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    command_name = "add";
                    Log.d(TAG,"SHOW:"+command_name);
                    test=1;
                }
            });
            commands.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    command_name = "command";
                    Log.d(TAG,"SHOW:"+command_name);
                    test=1;
                }
            });

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
            send2("d7828ff13d52a90c8c6cff651fd8ec1e");

            //send my ID pass
            recv2();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            send2(owner_ID);
            recv2();
            send2(owner_pass);

            //start use
            recv2();
            Log.d("server: success", "good!");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(isConnected){
                if(command_name.equals("add")&&test==1){
                    send2("add");
                    recv2();
                    send2("phone_tenant");
                    recv2();
                    send2("123456");
                    recv2();
                    send2("9");
                    test=0;
                }
                else if(command_name.equals("command")&&test==1){
                    send2("command");
                    recv2();
                    send2("get video");
//                    recv2();
                    Intent intent = new Intent(ToolsActivity.this, StreamActivity.class);
                    intent.putExtra("web", web);
                    startActivity(intent);
                    test=0;
                }
            }
//            try {
//                server_socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        }

    }
}