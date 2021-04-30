package com.keistudio.multithreaddownloader.multi_thread_downloader.downloader;

import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileDownloader {


    private static final String TAG = FileDownloader.class.getSimpleName();

    private static final int RESPONSEOK = 200;//响应码200即访问成功
    private Context context;//应用程序上下文
    private boolean exited;//停止下载标志
    private int downloadSize = 0;//已下载文件长度
    private int fileSize = 0;//原始文件长度

    private FileService fileService;//获取本地数据库的业务Bean
    private DownloadThread[] threads;//下载线程池(根据线程数设置)
    private File saveFile;//数据保存到本地文件
    private Map<Integer, Integer> data = new ConcurrentHashMap<>();//缓存各线程下载的长度
    private int block;//每条线程下载的长度
    private String downloadUrl;//下载路径


//    public static FileDownloader newInstance(int threadCount) {
//        FileDownloader fileDownloader = new FileDownloader();
//        fileDownloader.threadCount = threadCount;
//
//        return fileDownloader;
//    }


    /**
     * 获取线程数
     */
    public int getThreadSize() {
        return threads.length;
    }

    /**
     * 退出下载
     */
    public void exit() {
        this.exited = true;
    }

    public boolean getExited() {
        return this.exited;
    }

    /**
     * 获取文件大小
     */
    public int getFileSize() {
        return fileSize;
    }

    /**
     * 累计已下载大小
     * <p>
     * (每条下载线程都会调用该方法进行累计)
     */
    protected synchronized void append(int size) { //使用同步关键字解决并发访问问题

        downloadSize += size; //把实时下载的长度加入到总长度
    }

    /**
     * 更新指定线程最后下载的位置
     */
    protected synchronized void update(int threadid, int pos) {
        this.data.put(threadid, pos);
        this.fileService.update(downloadUrl, threadid, pos);
    }

    /**
     * 构建文件下载器
     */
    public FileDownloader(Context context, String downloadUrl, File fileSaveDir, int threadNum) {
        try {
            this.context = context;
            this.downloadUrl = downloadUrl;

            fileService = new FileService(this.context);

            //根据下载路径实例化URL
            URL url = new URL(this.downloadUrl);
            //如果指定的文件不存在，则创建目录(可创建多层目)
            if (!fileSaveDir.exists()) {
                fileSaveDir.mkdirs();
            }

            this.threads = new DownloadThread[threadNum];//根据线程数创建下载线程池

            //网络部分
            String filename = httpSetting(url);

            this.saveFile = new File(fileSaveDir, filename);
            //获取下载记录
            Map<Integer, Integer> logdata = fileService.getData(downloadUrl);
            if (logdata.size() > 0) {
                //存在下载记录
                for (Map.Entry<Integer, Integer> entry : logdata.entrySet()) {
                    data.put(entry.getKey(), entry.getValue());//把各条线程已经下载的数据长度放入data中
                }
            }
            if (this.data.size() == this.threads.length) {//如果已经下载的数据的线程数和现在设置的线程数相同时，则计算所有线程已经下载的数据总长度

//                for (int i = 0; i < this.threads.length; i++) {
//                    //计算已经下载的数据之和
//                    this.downloadSize += this.data.get(i+1);
//                }
                for (Map.Entry<Integer, Integer> entry : this.data.entrySet()) {
                    this.downloadSize += entry.getValue();
                }

                print("已经下载的长度：" + this.downloadSize + "个字节");
            }

            int l = this.fileSize / this.threads.length;
            this.block = (this.fileSize % this.threads.length) == 0 ? l : l + 1;


        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            throw new RuntimeException("can not connect this url");
        }
    }

    /**
     * 初始化网络连接部分
     *
     * @param url
     * @throws IOException
     */
    private String httpSetting(URL url) throws IOException {
        //建立一个远程连接句柄(为真正连接)
        HttpURLConnection connect = (HttpURLConnection) url.openConnection();
        connect.setConnectTimeout(5 * 1000);//连接超时时间 5s
        connect.setRequestMethod("GET");
        connect.setRequestProperty("Accept", "*/*");//设置客户端可以接受的媒体类型
        connect.setRequestProperty("Accept-Language", "zh-CN");//设置客户端语言
        connect.setRequestProperty("Referer", downloadUrl);//设置请求的来源页面，便于服务的进行来源统计
        connect.setRequestProperty("Charset", "UTF-8");//设置客户端编码
        connect.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");//设置用户代理
        connect.setRequestProperty("Connection", "Keep-Alive");//设置connection连接方式
        connect.connect();//和远程资源建立真正的链接，但上午返回的数据流
        printResponseHeader(connect);//答应返回的HTTP头字段集合
        if (connect.getResponseCode() == RESPONSEOK) {//此处的请求会打开返回流并获取返回的状态码，用于检查是否请求成功
            this.fileSize = connect.getContentLength();
            if (this.fileSize <= 0) {
                throw new RuntimeException("Unknown file size");
            }
            String filename = getFileName(connect);
            return filename;
        } else {
            Log.e(TAG, "服务器响应错误：" + connect.getResponseCode() + " - " + connect.getResponseMessage());
            throw new RuntimeException("server response error");
        }

    }

    private String getFileName(HttpURLConnection connect) {
        String filename = this.downloadUrl.substring(this.downloadUrl.lastIndexOf('/' + 1));
        if (filename == null || "".equals(filename.trim())) {

            for (int i = 0; ; i++) {//无限循环

                String mine = connect.getHeaderField(i);//从返回的流中获取特定索引的头字段值
                if (mine == null) break;//如果遍历到返回头的末尾处，退出循环

                //获取content-disposition返回头字段，里面可能会包含文件名
                if ("content-disposition".equals(connect.getHeaderFieldKey(i).toLowerCase())) {
                    //使用正则表达式查询文件名
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
                    //如果有符合正则表达式的字符串
                    if (m.find()) return m.group(1);
                }
            }

            filename = UUID.randomUUID() + ".tmp";

        }
        return filename;
    }


    /**
     * 开始下载文件
     *
     * @param listener 监听下载数量的变化，如不需要实时了解下载数量可以设为null
     * @return 已下载文件的大小
     * @throws Exception
     */
    public int download(DownloadProgressListener listener) throws Exception {

        try {
            RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rwd");//可读写，每当文件内容改变，会同步写到本地文件？
            if (this.fileSize > 0) {
                randOut.setLength(this.fileSize);
                randOut.close();//关闭该文件，是设置生效
                URL url = new URL(this.downloadUrl);
                if (this.data.size() != this.threads.length) {//若原先未下载或和现在的下载线程数不一致
                    //清空原下载数据
                    this.data.clear();
                    for (int i = 0; i < this.threads.length; i++) {
                        //初始化每条线程已经下载的长度为0(线程id为1、2、3....)
                        this.data.put(i + 1, 0);
                    }
                    this.downloadSize = 0;
                }
                //开启线程进行下载
                for (int i = 0; i < this.threads.length; i++) {
                    int threadId = i + 1;
                    //线程id=i+1的线程开始下载业务
                    int downloadLength = this.data.get(threadId);//通过特定的线程id获取该线程已经下载的数据长度
                    if (downloadLength < this.block && this.downloadSize < this.fileSize) {//判断当前线程是否已经完成下载：当前线程未下载完成且总文件未下载完成
                        //未完成，则继续下载

                        //初始化特定id的线程
                        this.threads[i] = new DownloadThread(saveFile, url, block, threadId, downloadLength, FileDownloader.this);
                        this.threads[i].setPriority(7);
                        this.threads[i].start();//启动线程

                    } else {
                        //当前线程已下载完毕
                        this.threads[i] = null;//当未null，表明线程已下载完毕
                    }


                }

                //如果存在下载记录，删除并重新添加
                fileService.delete(this.downloadUrl);
                //把已经下载的数据实时写入数据库
                fileService.save(this.downloadUrl, this.data);
                boolean notFinished = true;
                //在线程中进行轮询
                while (notFinished) {
                    Thread.sleep(900);
                    notFinished = false;//假定全部线程下载完成
                    for (int i = 0; i < this.threads.length; i++) {
                        //若特定id的线程未下载完
                        DownloadThread thread = this.threads[i];
                        if (thread != null && !thread.isFinished()) {
                            notFinished = true;

                            if (thread.getDownloadedLength() == -1) {//如果下载失败，再重新在已经下载的数据长度基础上下载
                                thread = new DownloadThread(saveFile, url, block, i + 1, this.data.get(i + 1), this);
                                thread.setPriority(7);
                                thread.start();
                            }
                        }
                    }

                    print("实时更新 download size = " + downloadSize + "字节");
                    if (listener != null) {
                        listener.onDownloadSize(this.downloadSize);
                    }
                }
            }

            //下载完成删除记录
            if (downloadSize == this.fileSize) {
                fileService.delete(downloadUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("File download error");
        }
        return this.downloadSize;

    }

    /**
     * 获取响应头字段
     *
     * @param http HttpURLConnection对象
     * @return 头字段map
     */
    private Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<>();//使用LinkedHashMap保证写入和遍历的时候顺序相同，且运行空值
        for (int i = 0; ; i++) {//无限循环，因为不知道头字段的数量
            String filedValue = http.getHeaderField(i);
            if (filedValue == null) break;//当遍历到没有值了，表明头字段遍历完毕
            header.put(http.getHeaderFieldKey(i), filedValue);
        }
        return header;

    }


    private void printResponseHeader(HttpURLConnection connect) {
        Map<String, String> headers = getHttpResponseHeader(connect);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            String msg = key + entry.getValue();
            print(msg);
        }
    }

    private void print(String msg) {
        Log.i(TAG, msg);
    }


}
