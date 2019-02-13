package com.rendebiao.download;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by DB on 2017/7/13.
 */

class DownloadTask implements Runnable {

    private static HashMap<String, DownloadTask> downloadTasks = new HashMap<>();
    private File file;
    private String name;
    private boolean pause;
    private boolean cancel;
    private long lastLength;
    private Exception exception;
    private DownloadInfo downloadInfo;
    private DownloadConfig downloadConfig;
    private DownloadWriter downloadWriter;
    private DownloadReader[] downloadReaders;
    private DownloadListener downloadListener;
    private BlockingQueue<DownloadData> readQueue;
    private BlockingQueue<DownloadData> writeQueue;

    public DownloadTask(DownloadInfo downloadInfo, DownloadConfig downloadConfig, DownloadListener downloadListener) {
        this.downloadInfo = downloadInfo;
        this.downloadConfig = downloadConfig;
        this.downloadListener = downloadListener;
        file = new File(downloadInfo.getTempPath());
        downloadInfo.setStatus(DownloadStatus.WAIT);
        downloadTasks.put(downloadInfo.getPath(), this);
        name = "DownloadTask:" + downloadInfo.getFileName();
    }

    public static DownloadTask getTask(String path) {
        return downloadTasks.get(path);
    }

    @Override
    public void run() {
        DownloadConfig.log(name + " start");
        handStart();
        try {
            URL fileUrl = new URL(downloadInfo.getUrl());
            boolean supportRange = supportRange(fileUrl);
            DownloadConfig.log(name + " supportRange = " + supportRange);
            int length = getContentLength(fileUrl);
            DownloadConfig.log(name + " length = " + length);
            initDownloadParts(length, supportRange);
            readQueue = new LinkedBlockingQueue<>();
            writeQueue = new LinkedBlockingQueue<>(downloadInfo.getDownloadParts().size() * 2);
            startDownloadWriter(file, length);
            startDownloadReaders(fileUrl);
            boolean finished = false;
            boolean readFinish = false;
            long time = System.currentTimeMillis();
            while (!finished) {
                updateException();
                if ((exception != null || pause || cancel)) {
                    for (int i = 0; i < downloadReaders.length; i++) {
                        if (downloadReaders[i] != null) {
                            downloadReaders[i].cancel();
                        }
                    }
                    while (!finished) {
                        Thread.sleep(10);
                        finished = isReadFinish(downloadReaders);
                    }
                    finished = false;
                    downloadWriter.cancel();
                    while (!finished) {
                        Thread.sleep(10);
                        finished = downloadWriter.isFinish();
                    }
                } else {
                    if (!readFinish) {
                        readFinish = isReadFinish(downloadReaders);
                        if (readFinish) {
                            DownloadConfig.log(name + " readFinish");
                            downloadWriter.setReadFinish(true);
                        }
                    }
                    long sleep = 500 - System.currentTimeMillis() + time;
                    if (sleep > 0) {
                        Thread.sleep(sleep);
                    }
                    time = System.currentTimeMillis();
                    finished = readFinish && downloadWriter.isFinish();
                }
                handProgress();
            }
        } catch (Exception e) {
            exception = e;
            e.printStackTrace();
        } finally {
            if (exception != null) {
                handFail(exception);
            } else if (pause) {
                handPause();
            } else if (cancel) {
                handCancel();
            } else {
                handSuccess();
            }
            downloadTasks.remove(downloadInfo.getPath());
            while (readQueue.size() > 0) {
                readQueue.poll().recycle();
            }
        }
    }

    private void updateException() {
        if (exception == null) {
            exception = downloadWriter.getException();
        }
        if (exception == null) {
            for (int i = 0; i < downloadReaders.length; i++) {
                if (downloadReaders[i] != null && downloadReaders[i].getException() != null) {
                    exception = downloadReaders[i].getException();
                    break;
                }
            }
        }
    }

    private boolean supportRange(URL fileUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
        conn.setAllowUserInteraction(true);
        conn.setRequestProperty("Range", "bytes=" + 0 + "-" + 1);
        boolean support = conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL;
        conn.disconnect();
        return support;
    }

    private int getContentLength(URL fileUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20000);
        int code = conn.getResponseCode();
        int length = conn.getContentLength();
        conn.disconnect();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("responseCode = " + code);
        }
        return length;
    }

    private void initDownloadParts(int fileLength, boolean supportRange) throws IOException {
        DownloadConfig.log(name + " initDownloadParts");
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (!supportRange || !file.exists() || (downloadInfo.getTotalLength() > 0 && downloadInfo.getTotalLength() != fileLength) ||
                (downloadInfo.getLastModified() > 0 && Math.abs(downloadInfo.getLastModified() - file.lastModified()) > 1000)) {
            DownloadConfig.log(name + " reset downloadInfo " + file.exists() + " " + downloadInfo.getTotalLength() + " " + fileLength + " " + downloadInfo.getLastModified() + " " + file.lastModified());
            file.delete();
            file.createNewFile();
            downloadInfo.clearDownloadParts();
        }
        if (downloadInfo.getDownloadParts().size() == 0) {
            int partCount = supportRange ? downloadConfig.getDownloadPartRule().getPartCounts(fileLength) : 1;
            int partLength = (fileLength % partCount) == 0 ? (fileLength / partCount) : (fileLength / partCount + 1);
            for (int i = 0; i < partCount; i++) {
                DownloadPart downloadPart = new DownloadPart(i, i * partLength, Math.min((i + 1) * partLength, fileLength) - 1);
                downloadInfo.addDownloadPart(downloadPart);
            }
            DownloadConfig.log(name + " init downloadInfo partCount = " + partCount);
        }
    }

    private void startDownloadReaders(URL fileUrl) {
        DownloadConfig.log(name + " startDownloadReaders");
        downloadReaders = new DownloadReader[downloadInfo.getDownloadParts().size()];
        boolean single = downloadReaders.length == 1;
        for (int i = 0; i < downloadReaders.length; i++) {
            if (!downloadInfo.getDownloadPart(i).isDownloadComplete()) {
                downloadReaders[i] = new DownloadReader(fileUrl, downloadInfo.getDownloadPart(i), single, downloadConfig.getBufferSize(), readQueue, writeQueue);
                downloadReaders[i].setName("DownloadReader:" + downloadInfo.getFileName() + "-" + i);
                downloadReaders[i].setDaemon(true);
                downloadReaders[i].setPriority(8);
                downloadReaders[i].start();
            }
        }
    }

    private void startDownloadWriter(File file, int fileLength) {
        downloadWriter = new DownloadWriter(file, readQueue, writeQueue, fileLength, false);
        downloadWriter.setName("DownloadWriter:" + downloadInfo.getFileName());
        downloadWriter.setDaemon(true);
        downloadWriter.start();
    }

    private boolean isReadFinish(DownloadReader[] downloadReaders) {
        for (int i = 0; i < downloadReaders.length; i++) {
            if (downloadReaders[i] != null && !downloadReaders[i].isFinish()) {
                return false;
            }
        }
        return true;
    }

    public boolean pause() {
        if (!pause) {
            pause = true;
            return true;
        }
        return false;
    }

    public boolean cancel() {
        if (!cancel) {
            cancel = true;
            return true;
        }
        return false;
    }

    private void handStart() {
        DownloadConfig.log(name + " handStart");
        downloadInfo.setStatus(DownloadStatus.RUNNING);
        downloadListener.onDownloadStart(downloadInfo);
    }

    private void handProgress() {
        long curLength = downloadInfo.getCurLength();
        if (lastLength != curLength) {
            DownloadConfig.log(name + " handProgress " + curLength + "/" + downloadInfo.getTotalLength());
            downloadInfo.setLastModified(file.lastModified());
            downloadListener.onDownloadProgress(downloadInfo);
            lastLength = curLength;
        }
    }

    private void handPause() {
        DownloadConfig.log(name + " handPause " + file.lastModified());
        downloadInfo.setStatus(DownloadStatus.PAUSE);
        downloadInfo.setLastModified(file.lastModified());
        downloadListener.onDownloadPause(downloadInfo);
    }

    private void handCancel() {
        DownloadConfig.log(name + " handCancel");
        if (file.exists()) {
            file.delete();
        }
        downloadListener.onDownloadCancel(downloadInfo);
    }

    private void handSuccess() {
        DownloadConfig.log(name + " handSuccess");
        File target = new File(downloadInfo.getPath());
        if (target.exists()) {
            target.delete();
        }
        file.renameTo(target);
        downloadInfo.setStatus(DownloadStatus.SUCCESS);
        downloadInfo.setLastModified(target.lastModified());
        downloadListener.onDownloadSuccess(downloadInfo);
    }

    private void handFail(final Exception e) {
        DownloadConfig.log(name + " handFail " + e.getMessage());
        downloadInfo.setStatus(DownloadStatus.FAIL);
        downloadInfo.setLastModified(file.lastModified());
        downloadListener.onDownloadFail(downloadInfo, e);
    }
}
