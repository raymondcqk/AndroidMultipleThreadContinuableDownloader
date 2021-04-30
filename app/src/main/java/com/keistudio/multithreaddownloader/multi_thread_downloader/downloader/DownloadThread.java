package com.keistudio.multithreaddownloader.multi_thread_downloader.downloader;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadThread extends Thread {

    private static final String TAG = DownloadThread.class.getSimpleName();

    private File saveFile; //下载的数据要保存到的文件
    private URL downloadUrl;
    private int block;
    private int threadId = -1;
    private int downloadedLength; //该线程已经下载的数据长度
    private boolean finished = false;//该线程是否完成下载的标志
    private FileDownloader fileDownloader;//文件下载器

    public DownloadThread(File saveFile, URL downloadUrl, int block, int threadId, int downloadedLength, FileDownloader fileDownloader) {
        this.saveFile = saveFile;
        this.downloadUrl = downloadUrl;
        this.block = block;
        this.threadId = threadId;
        this.downloadedLength = downloadedLength;
        this.fileDownloader = fileDownloader;
    }

    @Override
    public void run() {

        if (downloadedLength < block) {
            try {
                HttpURLConnection http = (HttpURLConnection) downloadUrl.openConnection();
                http.setConnectTimeout(5 * 1000);
                http.setRequestMethod("GET");
                http.setRequestProperty("Accept", "*/*");
                http.setRequestProperty("Accept-Language", "zh-CN");
                http.setRequestProperty("Referer", downloadUrl.toString());
                http.setRequestProperty("Charset", "UTF-8");

                int startPos = block * (threadId - 1) + downloadedLength;
                int endPos = block * threadId - 1;//下一个线程起始位置的前一位

                //设置获取实体数据的范围。如果超过了实体数据的大小会自动返回实际的数据大小
                http.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
                http.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");//设置用户代理
                http.setRequestProperty("Connection", "Keep-Alive");//长连接

                InputStream is = http.getInputStream();//获取远程连接的输入流
                byte[] buffer = new byte[20480];//设置本地数据缓存的大小为1MB
                int offset = 0;//每次读取的数据量
                print("Thread " + this.threadId + " start to download from postion " + startPos);
                RandomAccessFile threadFile = new RandomAccessFile(this.saveFile, "rwd");
                threadFile.seek(startPos);

                //用户为要求下载器停止下载 且 请求的数据未达到末尾
                while (!fileDownloader.getExited() && (offset = is.read(buffer, 0, 20480)) != -1) {
                    threadFile.write(buffer, 0, offset);
                    downloadedLength += offset;
                    fileDownloader.update(this.threadId, downloadedLength);
                    fileDownloader.append(offset);
                }
                //线程下载完成或用户终止下载
                threadFile.close();
                is.close();

                if (fileDownloader.getExited()) {
                    //是用户终止的下载 - 未下载完
                    print("thread " + threadId + " has bean paused!");
                } else {
                    //线程下载完
                    print("thread " + threadId + " download finish oh yeah !!!");
                }
                this.finished = true;//设置结束标志位true，表示当前线程结束下载，无论是下载完还是用户终止


            } catch (IOException e) {
                this.downloadedLength = -1;//若出现异常，设置该线程已经下载的长度为-1
                print("Thread " + this.threadId + " : " + e);
            }
        }

    }

    public boolean isFinished() {
        return finished;
    }


    public int getDownloadedLength() {
        return downloadedLength;
    }


    private void print(String msg) {
        Log.i(TAG, msg);
    }

}
