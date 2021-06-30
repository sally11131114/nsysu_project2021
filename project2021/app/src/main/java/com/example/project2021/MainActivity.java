package com.example.project2021;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button mBtn_On, mBtn_discover;

    BluetoothAdapter mBlueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn_On         = findViewById(R.id.btn_on);
        mBtn_discover   = findViewById(R.id.btn_discoverable);
        mBlueAdapter    = BluetoothAdapter.getDefaultAdapter();

        mBtn_On.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBlueAdapter == null ){
                    Toast.makeText(v.getContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                }
                else {
                    //open bluetooth
                    if (!mBlueAdapter.isEnabled()) {
                        Toast.makeText(v.getContext(), "Turning ON...", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 0);
                    }
                    else {
                        Toast.makeText(v.getContext(), "Bluetooth is already on.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mBtn_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //discoverable
                if(!mBlueAdapter.isDiscovering()){
                    Toast.makeText(v.getContext(), "Press  allow to make your device discoverable.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, 1);
                }
                else{
                    Toast.makeText(v.getContext(), "Your device is already discoverable.", Toast.LENGTH_SHORT).show();
                }
            }
        });


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
}