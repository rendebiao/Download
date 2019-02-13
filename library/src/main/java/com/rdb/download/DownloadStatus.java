package com.rdb.download;

/**
 * Created by DB on 2017/8/10.
 */

public enum DownloadStatus {
    INIT(0), WAIT(1), RUNNING(2), PAUSE(3), SUCCESS(4), FAIL(5), UNKNOW(-1);

    int status;

    DownloadStatus(int status) {
        this.status = status;
    }

    public static DownloadStatus fromStatus(int status) {
        for (DownloadStatus downloadStatus : values()) {
            if (downloadStatus.getStatus() == status) {
                return downloadStatus;
            }
        }
        return UNKNOW;
    }

    public int getStatus() {
        return status;
    }
}
