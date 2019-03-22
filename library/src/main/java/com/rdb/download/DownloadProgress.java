package com.rdb.download;

class DownloadProgress implements Runnable {

    private DownloadInfo downloadInfo;
    private DownloadListener downloadListener;

    public DownloadProgress(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    @Override
    public void run() {
        if (downloadListener != null) {
            downloadListener.onDownloadProgress(downloadInfo);
        }
    }
}
