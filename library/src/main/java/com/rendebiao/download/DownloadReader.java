package com.rendebiao.download;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * Created by DB on 2017/7/13.
 */
class DownloadReader extends Thread {

    private int bufferSize;
    private boolean single;
    private boolean cancel;
    private boolean finish;
    private URL downloadUrl;
    private Exception exception;
    private DownloadPart downloadPart;
    private BlockingQueue<DownloadData> readQueue;
    private BlockingQueue<DownloadData> writeQueue;

    public DownloadReader(URL url, DownloadPart downloadPart, boolean single, int bufferSize, BlockingQueue<DownloadData> readQueue, BlockingQueue<DownloadData> writeQueue) {
        this.downloadUrl = url;
        this.downloadPart = downloadPart;
        this.single = single;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        DownloadConfig.log(getName() + " start");
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) downloadUrl.openConnection();
            int startPos = downloadPart.getCurPosition();
            int endPos = downloadPart.getEndPosition();
            if (single && downloadPart.downloadLength() == 0) {
                if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException(getName() + " responseCode = " + httpURLConnection.getResponseCode());
                }
            } else {
                httpURLConnection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
                if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) {
                    throw new RuntimeException(getName() + " responseCode = " + httpURLConnection.getResponseCode());
                }
            }
            int length;
            int dataLength;
            int emptyLength;
            int offset = startPos;
            DownloadData downloadData;
            long[] times = new long[5];
            InputStream inputStream = httpURLConnection.getInputStream();
            while (true) {
                if (cancel) {
                    DownloadConfig.log(getName() + " cancel");
                    break;
                }
                downloadData = readQueue.poll();
                if (downloadData == null) {
                    downloadData = new DownloadData(bufferSize);
                } else {
                    downloadData.setLength(0);
                }
                downloadData.setOffset(offset);
                downloadData.setDownloadPart(downloadPart);
                dataLength = 0;
                times[0] = System.currentTimeMillis();
                while ((emptyLength = downloadData.getBytes().length - dataLength) > 0) {
                    if ((length = inputStream.read(downloadData.getBytes(), dataLength, emptyLength)) != -1) {
                        dataLength += length;
                    } else {
                        break;
                    }
                }
                times[1] = System.currentTimeMillis();
                if (dataLength > 0) {
                    downloadData.setLength(dataLength);
                    offset += dataLength;
                    writeQueue.put(downloadData);
                    times[2] = System.currentTimeMillis();
                    times[3] = times[1] - times[0];
                    times[4] = times[2] - times[1];
                    if (times[3] > 500 || times[4] > 500) {
                        DownloadConfig.log(getName() + " read length = " + dataLength + " " + times[3] + " " + times[4]);
                    }
                } else {
                    readQueue.put(downloadData);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.exception = e;
            DownloadConfig.log(getName() + " exception " + e.getMessage());
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            finish = true;
            DownloadConfig.log(getName() + " end");
        }
    }

    public boolean cancel() {
        if (!cancel) {
            cancel = true;
            return true;
        }
        return false;
    }

    public boolean isFinish() {
        return finish;
    }

    public Exception getException() {
        return exception;
    }
}
