package com.example.tenant;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

public class StreamActivity extends AppCompatActivity {
    String web;
    VideoView stream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        web=getIntent().getStringExtra("web");
        stream = (VideoView)findViewById(R.id.vv);
//        ss     = (Button) findViewById(R.id.start_stream);
        String path =web;
        Log.d("web", "" + web);
        Uri uri = Uri.parse(path);
        stream.setMediaController(new MediaController(this));
        stream.setVideoURI(uri);
        stream.requestFocus();
        stream.start();
    }
}