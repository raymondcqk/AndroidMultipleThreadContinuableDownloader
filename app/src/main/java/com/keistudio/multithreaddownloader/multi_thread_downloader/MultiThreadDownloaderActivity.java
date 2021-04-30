package com.keistudio.multithreaddownloader.multi_thread_downloader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.keistudio.multithreaddownloader.R;
import com.keistudio.multithreaddownloader.multi_thread_downloader.downloader.DownloadProgressListener;
import com.keistudio.multithreaddownloader.multi_thread_downloader.downloader.FileDownloader;

import java.io.File;

public class MultiThreadDownloaderActivity extends AppCompatActivity {

    public static final String TAG = MultiThreadDownloaderActivity.class.getSimpleName();


    private static final int PROCESSING = 1;//Message标志 - 正在下载实时数据传输
    private static final int FAILURE = -1;//Message标志 - 下载失败

    private EditText mEdtUrl;
    private TextView mTvResult;
    private ProgressBar mProgressbar;

    private Handler handler = new UIHandler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_thread_downloader);

        mEdtUrl = findViewById(R.id.edt_path);
        mTvResult = findViewById(R.id.tv_result);
        mProgressbar = findViewById(R.id.progress_bar);


    }

    public void download(View view) {

        Log.i(TAG, "click start download");

//        startDownload(mEdtUrl.getText().toString().trim());

        String path = mEdtUrl.getText().toString().trim();


        //检查SD卡是否存在(内置存储也是这么处理吗？)
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            /**
             * 用新的Android SDK 29编译的时候，Studio提示Environment.getExternalStorageDirectory()过时了，要用Context#getExternalFilesDir代替
             * Android Q以后Environment.getExternalStorageDirectory()返回的路径可能无法直接访问
             * 所以改成了Context#getExternalFilesDir
             */

            /**
             * 得到的路径如下：
             *
             * /storage/emulated/0/Android/data/yourPackageName/files
             *
             * 这个目录会在应用被卸载的时候删除，而且访问这个目录不需要动态申请STORAGE权限。
             *
             * 如果这个目录不存在，系统会自动帮你创建，看下源码
             */
            File saveDir = getApplicationContext().getExternalFilesDir(null);
            startDownload(path, saveDir);

        } else {
            Toast.makeText(this, R.string.sdcard_error, Toast.LENGTH_LONG).show();
        }

    }

    private void startDownload(final String url, File saveDir) {
        Log.i(TAG, "start download url: " + url);
        Log.i(TAG, "save dir: " + saveDir.getAbsolutePath());

        /**
         * 耗时操作要在子线程执行
         * 否则阻塞主线程
         */
        task = new DownloadTask(url, saveDir);
        new Thread(task).start();


    }

    public void stopDownload(View view) {

        Log.i(TAG, "click stop download");

        exit();//停止下载

    }

    private void exit() {
        if (task != null)
            task.exit();

    }

    private final class UIHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROCESSING:

                    int size = msg.getData().getInt("size");
                    int fileSize = msg.getData().getInt("fileSize");
                    mProgressbar.setProgress(size);
                    mTvResult.setText((size * 100 / fileSize) + "%");

                    break;
                case FAILURE:
                    Toast.makeText(getApplicationContext(), R.string.download_error, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    /**
     * 耗时操作要在子线程执行
     * 否则阻塞主线程
     */

    private DownloadTask task;//声明下载执行者


    private class DownloadTask implements Runnable {

        private String url;//下载地址
        private File saveDir;//文件保存目录
        private FileDownloader downloader;//文件下载器(下载线程的容器)
        private DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void onDownloadSize(int size) {

                Message msg = new Message();
                msg.what = PROCESSING;
                msg.getData().putInt("size", size);//把文件下载的size设置进Message队列
                msg.getData().putInt("fileSize", downloader.getFileSize());//把文件下载的size设置进Message队列
                handler.sendMessage(msg);//通过handler发送消息到消息队列

            }
        };


        public DownloadTask(String url, File saveDir) {
            this.url = url;
            this.saveDir = saveDir;

        }

        @Override
        public void run() {
            try {
                this.downloader = new FileDownloader(MultiThreadDownloaderActivity.this, url, saveDir, 6);
                mProgressbar.setMax(this.downloader.getFileSize());//设置进度条的最大刻度
                this.downloader.download(listener);
            } catch (Exception e) {
                e.printStackTrace();
                handler.sendMessage(handler.obtainMessage(FAILURE));

            }

        }

        public void exit() {
            if (downloader != null)
                downloader.exit();

        }
    }
}
