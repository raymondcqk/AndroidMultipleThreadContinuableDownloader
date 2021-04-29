package com.keistudio.multithreaddownloader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.keistudio.multithreaddownloader.multi_thread_downloader.MultiThreadDownloaderActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 多线程下载器
     * @param view
     */
    public void multiThreadDownloader(View view) {
        Intent intent = new Intent(this, MultiThreadDownloaderActivity.class);
        startActivity(intent);
    }
}
