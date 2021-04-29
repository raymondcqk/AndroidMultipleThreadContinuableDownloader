package com.keistudio.multithreaddownloader.multi_thread_downloader.downloader;

import android.content.Context;

/**
 *
 * 业务Bean，实现对数据的操作
 * 数据库业务操作类
 *
 * @author keihong.chan
 */
public class FileService {

    private DBOpenHelper openHelper;

    public FileService(Context context) {
        openHelper = new DBOpenHelper(context);
    }


}
