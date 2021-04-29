package com.keistudio.multithreaddownloader.multi_thread_downloader.downloader;

public class FileDownloader {


    private int threadCount;



    public static FileDownloader newInstance(int threadCount){
        FileDownloader fileDownloader = new FileDownloader();
        fileDownloader.threadCount = threadCount;

        return fileDownloader;
    }


    public void download(String url, String dir) {

    }
}
