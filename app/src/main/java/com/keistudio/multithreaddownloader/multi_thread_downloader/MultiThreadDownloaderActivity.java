package com.keistudio.multithreaddownloader.multi_thread_downloader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.keistudio.multithreaddownloader.R;
import com.keistudio.multithreaddownloader.multi_thread_downloader.downloader.FileDownloader;

public class MultiThreadDownloaderActivity extends AppCompatActivity {

    public static final String TAG = MultiThreadDownloaderActivity.class.getSimpleName();

    private EditText mEdtUrl;

    private Handler handler = new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_thread_downloader);

        mEdtUrl = findViewById(R.id.edt_path);
    }

    public void download(View view) {

        Log.i(TAG,"click start download");

        startDownload(mEdtUrl.getText().toString().trim());




    }

    private void startDownload(final String url) {
        Log.i(TAG,"start download url: "+url);
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileDownloader fileDownloader = FileDownloader.newInstance();
                fileDownloader.download(url,"keihongdata");
            }
        }).run();
    }

    public void stopDownload(View view) {

        Log.i(TAG,"click stop download");

    }
}
