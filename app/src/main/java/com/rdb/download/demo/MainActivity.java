package com.rdb.download.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rdb.download.DownloadConfig;
import com.rdb.download.DownloadManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DownloadManager.init(new DownloadConfig.Builder().build());
    }
}
