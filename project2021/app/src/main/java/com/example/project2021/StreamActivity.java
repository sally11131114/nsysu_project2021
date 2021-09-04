package com.example.project2021;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

public class StreamActivity extends AppCompatActivity {

    VideoView stream;
    Button ss;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        stream = (VideoView)findViewById(R.id.vv);
        ss     = (Button) findViewById(R.id.start_stream);
    }

    public void starts(View view) {
        String path ="rtsp://172.20.10.6:8554/unicast";
        Uri uri = Uri.parse(path);
        stream.setMediaController(new MediaController(this));
        stream.setVideoURI(uri);
        stream.requestFocus();
        stream.start();
    }
}