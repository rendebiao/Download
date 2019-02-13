package com.rendebiao.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rendebiao.download.DownloadConfig;
import com.rendebiao.download.DownloadManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DownloadManager.init(new DownloadConfig.Builder().build());
    }
}
