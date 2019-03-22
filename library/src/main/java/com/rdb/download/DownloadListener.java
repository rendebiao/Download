package com.rdb.download;

public class DownloadListener {

    private Object tag;

    public DownloadListener() {
    }

    public DownloadListener(Object tag) {
        this.tag = tag;
    }

    public final Object getTag() {
        return tag;
    }

    public void onDownloadStart(DownloadInfo downloadInfo) {

    }

    public void onDownloadProgress(DownloadInfo downloadInfo) {

    }

    public void onDownloadPause(DownloadInfo downloadInfo) {

    }

    public void onDownloadCancel(DownloadInfo downloadInfo) {

    }

    public void onDownloadSuccess(DownloadInfo downloadInfo) {

    }

    public void onDownloadFail(DownloadInfo downloadInfo, Exception e) {

    }
}
