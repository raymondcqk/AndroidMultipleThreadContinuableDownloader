package com.keistudio.multithreaddownloader.multi_thread_downloader.downloader;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * SQLite管理类：
 * 1. 数据库、表创建
 * 2. 数据库版本升级 - 表增删修改操作
 * @author keihong
 */
public class DBOpenHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "keihong.db";
    public static final int VERSION = 1;

    public DBOpenHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    /**
     * 建立数据库表
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
db.execSQL(" CREATE TABLE IF NOT EXISTS filedownlog (id integer primary key autoincrement," +
        " downpath varchar(300)," +
        " threadid integer," +
        " downlength integer) ");


    }

    /**
     * 当版本发送变化时系统会调用该方法
     * @param db
     * @param oldVer
     * @param newVer
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {



        //删除表(实际业务中需要备份数据库)
        db.execSQL(" DROP TABLE IF EXISTS filedownlog");

        //重新创建表，也可以根据业务需要创建新的数据库表
        onCreate(db);




    }
}
