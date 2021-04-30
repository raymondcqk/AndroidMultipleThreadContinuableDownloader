package com.keistudio.multithreaddownloader.multi_thread_downloader.downloader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务Bean，实现对数据的操作
 * 数据库业务操作类
 *
 * @author keihong.chan
 */
public class FileService {

    private DBOpenHelper openHelper;//声明数据库管理器

    public FileService(Context context) {
        openHelper = new DBOpenHelper(context);//根据上下文对象实例化数据库管理器
    }

    /**
     * 获取特定URI的全部线程已下载的文件长度
     *
     * @param path
     * @return
     */
    public Map<Integer, Integer> getData(String path) {
        //获取可读数据库句柄，一般情况下在该操作的内部实现中，其返回的其实是可写的数据库句柄
        SQLiteDatabase db = openHelper.getReadableDatabase();
        //根据下载路径查询所有线程下载数据，返回的Cursor指向第一条记录之前
        Cursor cursor = db.rawQuery("select threadid, downlength from filedownlog where downpath = ?",
                new String[]{path});
        //建立一个哈希表用于存放每条线程的已经下载长度
        Map<Integer, Integer> data = new HashMap<>();
        while (cursor.moveToNext()) {//当切换到下一个游标有数据时为true
            //把线程id和已经下载长度设置进data哈希表中
            data.put(cursor.getInt(0), cursor.getInt(1));
        }
        cursor.close();//关闭cursor，释放资源
        db.close();//关闭数据库
        return data;
    }

    /**
     * 保存每条线程已经下载文件长度
     *
     * @param path 下载的路径
     * @param map  线程id和已经下载文件长度的集合
     */
    public void save(String path, Map<Integer, Integer> map) {

        SQLiteDatabase db = openHelper.getWritableDatabase();//获取可写数据库句柄
        db.beginTransaction();//开始事务，因为要插入多批数据


        try {
            //采用For-Each的方式遍历数据集合
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                int threadid = entry.getKey();
                int downlength = entry.getValue();

                //插入特定下载路径、特定线程id、已经下载的数据
                db.execSQL("insert into filedownlog(downpath, threadid, downlength) " +
                        "values (?, ?, ?)", new Object[]{path, threadid, downlength});
            }

            //设置事务执行的标识为成功
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {//此部分代码肯定被执行，如果不杀死虚拟机
            //结束一个事务(如果事务执行标准为成功则提交，否则回滚)
            db.endTransaction();
        }
        db.close();//关闭数据库，释放相关资源

    }


    /**
     * 实时更新每条线程已经下载的文件长度
     *
     * @param path
     * @param threadid
     * @param pos
     */
    public void update(String path, int threadid, int pos) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("update filedownlog set downlength = ? " +
                "where downpath = ? and threadid = ?", new Object[]{pos, path, threadid});
        db.close();

        /**
         * 只插入一条时可以不使用事务
         */
    }

    public void delete(String path) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("delete from filedownlog where downpath = ? ", new Object[]{path});
        db.close();
    }


}
