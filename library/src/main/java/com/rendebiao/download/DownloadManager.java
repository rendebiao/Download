package com.rendebiao.download;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by DB on 2017/8/10.
 */

public class DownloadManager {

    private static DownloadManager instance;
    private static DownloadConfig downloadConfig;
    private Handler handler;
    private ExecutorService executor;
    private DownloadDBHelper downloadDBHelper;
    private DownloadController downloadController;
    private List<DownloadInfo> downloadInfos = new ArrayList<>();
    private HashMap<String, DownloadInfo> pathInfos = new HashMap<>();
    private SparseArray<List<DownloadInfo>> typeInfos = new SparseArray<>();
    private HashMap<String, List<DownloadInfo>> urlInfos = new HashMap<>();

    private DownloadManager(Context context) {
        if (downloadConfig == null) {
            downloadConfig = new DownloadConfig.Builder().build();
        }
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newFixedThreadPool(downloadConfig.getMaxTaskCount());
        this.downloadDBHelper = new DownloadDBHelper(context, downloadConfig.getDatabasePath());
        this.downloadController = new DownloadController(handler, downloadDBHelper);
        List<DownloadInfo> downloadInfos = downloadDBHelper.getDownloadInfos(false);
        Iterator<DownloadInfo> iterator = downloadInfos.iterator();
        while (iterator.hasNext()) {
            DownloadInfo downloadInfo = iterator.next();
            DownloadConfig.log("DownloadManager " + downloadInfo.toSimpleString());
            if (downloadInfo.getStatus() == DownloadStatus.UNKNOW) {
                downloadDBHelper.delete(downloadInfo.getPath());
                downloadInfo.deleteFile();
            } else {
                File file = new File(downloadInfo.getStatus() == DownloadStatus.SUCCESS ? downloadInfo.getPath() : downloadInfo.getTempPath());
                if (file.lastModified() == downloadInfo.getLastModified()) {
                    addDownloadInfo(downloadInfo);
                } else {
                    downloadDBHelper.delete(downloadInfo.getPath());
                    file.delete();
                }
            }
        }
    }

    public static void init(DownloadConfig config) {
        downloadConfig = config;
    }

    public synchronized static DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    public DownloadInfo downloadFile(File directory, String urlString, String fileName, DownloadListener downloadListener) {
        return downloadFile(-1, directory, urlString, fileName, downloadListener, null);
    }

    public DownloadInfo downloadFile(File directory, String urlString, DownloadFileNameBuilder nameBuilder, DownloadListener downloadListener) {
        return downloadFile(-1, directory, urlString, nameBuilder, downloadListener, null);
    }

    public DownloadInfo downloadFile(int type, File directory, String urlString, DownloadFileNameBuilder nameBuilder, DownloadListener downloadListener, String extension) {
        if (nameBuilder == null) {
            nameBuilder = DownloadFileNameBuilder.MILLISECOND_WITH_SUFFIX;
        }
        return downloadFile(type, directory, urlString, nameBuilder.buildName(urlString), downloadListener, extension);
    }

    public DownloadInfo downloadFile(int type, File directory, String urlString, String fileName, DownloadListener downloadListener, String extension) {
        if (directory == null || TextUtils.isEmpty(urlString) || TextUtils.isEmpty(fileName)) {
            throw new RuntimeException("some param is error: directory = " + directory + " urlString = " + urlString + " fileName = " + fileName);
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!directory.exists()) {
            throw new RuntimeException("directory not exists");
        }
        String filePath = directory.getAbsolutePath() + "/" + fileName;
        DownloadInfo downloadInfo = pathInfos.get(filePath);
        if (downloadInfo == null) {
            downloadInfo = new DownloadInfo(type, System.currentTimeMillis(), urlString, filePath, extension);
        } else if (!TextUtils.equals(urlString, downloadInfo.getUrl())) {
            cancleDownload(downloadInfo);
            downloadInfo = new DownloadInfo(type, System.currentTimeMillis(), urlString, filePath, extension);
        }
        startDownload(downloadInfo, downloadListener);
        return downloadInfo;
    }

    private void startDownload(DownloadInfo downloadInfo, DownloadListener downloadListener) {
        DownloadConfig.log("DownloadManager startDownload " + downloadInfo.getUrl() + "  " + downloadInfo.getCurLength());
        if (downloadInfo.getStatus() == DownloadStatus.WAIT) {

        } else if (downloadInfo.getStatus() == DownloadStatus.RUNNING) {

        } else {
            DownloadTask task = new DownloadTask(downloadInfo, downloadConfig, downloadController);
            downloadController.registerDownloadListener(downloadInfo, downloadListener);
            executor.submit(task);
            addDownloadInfo(downloadInfo);
        }
    }

    public boolean pauseDownload(DownloadInfo downloadInfo) {
        if (downloadInfo != null) {
            DownloadConfig.log("DownloadManager pauseDownload " + downloadInfo.getPath());
            DownloadTask task = DownloadTask.getTask(downloadInfo.getPath());
            if (task != null) {
                DownloadConfig.log("DownloadManager pauseDownload task " + task);
                return task.pause();
            }
        }
        return false;
    }

    public boolean resumeDownload(DownloadInfo downloadInfo, DownloadListener downloadListener) {
        if (downloadInfo != null) {
            DownloadConfig.log("DownloadManager resumeDownload " + downloadInfo.getUrl() + "  " + downloadInfo.getCurLength());
            DownloadTask task = DownloadTask.getTask(downloadInfo.getPath());
            if (task == null) {
                startDownload(downloadInfo, downloadListener);
                return true;
            }
        }
        return false;
    }

    public void cancleDownload(DownloadInfo downloadInfo) {
        if (downloadInfo != null) {
            DownloadConfig.log("DownloadManager cancleDownload " + downloadInfo.getUrl());
            DownloadTask task = DownloadTask.getTask(downloadInfo.getPath());
            delDownloadInfo(downloadInfo);
            if (task != null) {
                task.cancel();
            } else {
                downloadDBHelper.delete(downloadInfo.getPath());
            }
            downloadInfo.deleteFile();
        }
    }

    public void deleteDownloadInfo(DownloadInfo downloadInfo, boolean deleteFile) {
        if (downloadInfo != null) {
            DownloadConfig.log("DownloadManager deleteDownloadInfo " + downloadInfo.getUrl());
            DownloadTask task = DownloadTask.getTask(downloadInfo.getPath());
            delDownloadInfo(downloadInfo);
            if (task != null) {
                task.cancel();
            } else {
                downloadDBHelper.delete(downloadInfo.getPath());
                if (deleteFile) {
                    File file = new File(downloadInfo.getPath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
            File file = new File(downloadInfo.getTempPath());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void registerDownloadListener(DownloadInfo downloadInfo, DownloadListener downloadListener) {
        if (downloadInfo != null) {
            downloadController.registerDownloadListener(downloadInfo, downloadListener);
        }
    }

    public void unregisterDownloadListener(DownloadListener downloadListener) {
        downloadController.unregisterDownloadListener(downloadListener);
    }

    public void unregisterDownloadListenerByTag(Object tag) {
        downloadController.unregisterDownloadListenerByTag(tag);
    }

    private void addDownloadInfo(DownloadInfo downloadInfo) {
        List<DownloadInfo> infos = urlInfos.get(downloadInfo.getUrl());
        if (infos == null) {
            infos = new ArrayList<>();
            urlInfos.put(downloadInfo.getUrl(), infos);
        }
        infos.add(downloadInfo);
        infos = typeInfos.get(downloadInfo.getType());
        if (infos == null) {
            infos = new ArrayList<>();
            typeInfos.put(downloadInfo.getType(), infos);
            infos = urlInfos.get(downloadInfo.getUrl());
        }
        infos.add(downloadInfo);
        if (!downloadInfos.contains(downloadInfo)) {
            downloadInfos.add(0, downloadInfo);
        }
        pathInfos.put(downloadInfo.getPath(), downloadInfo);
    }

    private void delDownloadInfo(DownloadInfo downloadInfo) {
        if (urlInfos.containsKey(downloadInfo.getUrl())) {
            urlInfos.get(downloadInfo.getUrl()).remove(downloadInfo);
        }
        List<DownloadInfo> infos = typeInfos.get(downloadInfo.getType());
        if (infos != null) {
            infos.remove(downloadInfo);
        }
        pathInfos.remove(downloadInfo.getPath());
        downloadInfos.remove(downloadInfo);
    }


    public List<DownloadInfo> getDownloadInfos() {
        return new ArrayList<>(downloadInfos);
    }

    public List<DownloadInfo> getDownloadInfosByUrl(String url) {
        List<DownloadInfo> downloadInfos = new ArrayList<>();
        if (!TextUtils.isEmpty(url) && urlInfos.containsKey(url)) {
            downloadInfos.addAll(urlInfos.get(url));
        }
        return downloadInfos;
    }

    public List<DownloadInfo> getDownloadInfosByUrl(int type) {
        List<DownloadInfo> downloadInfos = typeInfos.get(type);
        if (downloadInfos == null) {
            downloadInfos = new ArrayList<>();
        }
        return downloadInfos;
    }

    public DownloadInfo getDownloadInfo(String url, String path) {
        DownloadInfo downloadInfo = getDownloadInfoByPath(path);
        if (downloadInfo != null && TextUtils.equals(downloadInfo.getUrl(), url)) {
            return downloadInfo;
        }
        return null;
    }

    public DownloadInfo getDownloadInfoByPath(String path) {
        return pathInfos.get(path);
    }
}
