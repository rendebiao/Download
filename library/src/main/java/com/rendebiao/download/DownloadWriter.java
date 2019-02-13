package com.rendebiao.download;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by DB on 2018/6/6.
 */

public class DownloadWriter extends Thread {

    private File file;
    private boolean mapped;
    private boolean cancel;
    private boolean finish;
    private long fileLengh;
    private boolean readFinish;
    private Exception exception;
    private List<DownloadData> cacheList = new ArrayList<>();
    private BlockingQueue<DownloadData> readQueue;
    private BlockingQueue<DownloadData> writeQueue;

    public DownloadWriter(File file, BlockingQueue<DownloadData> readQueue, BlockingQueue<DownloadData> writeQueue, long fileLengh, boolean mapped) {
        this.file = file;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
        this.fileLengh = fileLengh;
        this.mapped = mapped;
    }

    @Override
    public void run() {
        DownloadConfig.log(getName() + " start" + file.getPath());
        long length = 0;
        long time = System.currentTimeMillis();
        long writeTime = 0;
        boolean finished = false;
        RandomAccessFile randomAccessFile = null;
        MappedByteBuffer mappedByteBuffer = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rwd");
            if (mapped) {
                mappedByteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, fileLengh);
            }
            long[] times = new long[5];
            while (!finished) {
                if (cancel) {
                    DownloadConfig.log(getName() + " cancel");
                    break;
                }
                times[0] = System.currentTimeMillis();
                int size = writeQueue.size();
                finished = size == 0 && readFinish;
                if (size > 0) {
                    cacheList.clear();
                    for (int i = 0; i < size; i++) {
                        cacheList.add(writeQueue.take());
                    }
                    times[1] = System.currentTimeMillis();
                    for (int i = 0; i < size; i++) {
                        DownloadData data = cacheList.get(i);
                        long start = System.currentTimeMillis();
                        if (mapped) {
                            mappedByteBuffer.position(data.getOffset());
                            mappedByteBuffer.put(data.getBytes(), 0, data.getLength());
                        } else {
                            randomAccessFile.seek(data.getOffset());
                            randomAccessFile.write(data.getBytes(), 0, data.getLength());
                        }
                        writeTime += (System.currentTimeMillis() - start);
                        length += data.getLength();
                        data.getDownloadPart().addLength(data.getLength());
                        readQueue.put(data);
                    }
                    times[2] = System.currentTimeMillis();
                    times[3] = times[1] - times[0];
                    times[4] = times[2] - times[1];
                    if (times[3] > 100 || times[4] > 100) {
                        DownloadConfig.log(getName() + " write size = " + size + " " + times[3] + " " + times[4]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.exception = e;
            DownloadConfig.log(getName() + " exception " + e.getMessage());
        } finally {
            if (mappedByteBuffer != null) {
                mappedByteBuffer.force();
                mappedByteBuffer.clear();
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            finish = true;
            DownloadConfig.log(getName() + " end " + writeTime + "/" + (System.currentTimeMillis() - time) + " speed = " + (writeTime == 0 ? "" : (length / writeTime)));
        }
    }

    public void setReadFinish(boolean readFinish) {
        this.readFinish = readFinish;
    }

    public boolean isFinish() {
        return finish;
    }

    public Exception getException() {
        return exception;
    }

    public boolean cancel() {
        if (!cancel) {
            cancel = true;
            return true;
        }
        return false;
    }
}
