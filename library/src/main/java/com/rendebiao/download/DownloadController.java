package com.rendebiao.download;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by DB on 2017/8/12 0012.
 */

class DownloadController extends DownloadListener {

    private Handler handler;
    private DownloadDBHelper downloadDBHelper;
    private Map<DownloadInfo, DownloadProgress> progressMap = new HashMap<>();
    private Map<DownloadInfo, WeakReference<DownloadListener>> listenerMap = new HashMap<>();

    public DownloadController(Handler handler, DownloadDBHelper downloadDBHelper) {
        this.handler = handler;
        this.downloadDBHelper = downloadDBHelper;
    }

    public void registerDownloadListener(DownloadInfo downloadInfo, DownloadListener downloadListener) {
        if (downloadInfo != null) {
            addDownloadListener(downloadInfo, downloadListener);
        }
    }

    public void unregisterDownloadListener(DownloadListener downloadListener) {
        if (downloadListener != null) {
            synchronized (listenerMap) {
                Iterator<DownloadInfo> iterator = listenerMap.keySet().iterator();
                while (iterator.hasNext()) {
                    DownloadInfo downloadInfo = iterator.next();
                    DownloadListener listener = listenerMap.get(downloadInfo).get();
                    if (listener == downloadListener || listener == null) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void unregisterDownloadListenerByTag(Object tag) {
        if (tag != null) {
            synchronized (listenerMap) {
                Iterator<DownloadInfo> iterator = listenerMap.keySet().iterator();
                while (iterator.hasNext()) {
                    DownloadInfo downloadInfo = iterator.next();
                    DownloadListener listener = listenerMap.get(downloadInfo).get();
                    if (listener != null && listener.getTag() == tag) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void addDownloadListener(DownloadInfo downloadInfo, DownloadListener downloadListener) {
        synchronized (listenerMap) {
            listenerMap.put(downloadInfo, new WeakReference(downloadListener));
            if (progressMap.containsKey(downloadInfo)) {
                progressMap.get(downloadInfo).setDownloadListener(downloadListener);
            }
        }
    }

    private void delDownloadListener(DownloadInfo downloadInfo) {
        synchronized (listenerMap) {
            listenerMap.remove(downloadInfo);
        }
    }

    private void addDownloadProgress(DownloadInfo downloadInfo, DownloadProgress downloadProgress) {
        synchronized (progressMap) {
            progressMap.put(downloadInfo, downloadProgress);
        }
    }

    private void delDownloadProgress(DownloadInfo downloadInfo) {
        synchronized (progressMap) {
            progressMap.remove(downloadInfo);
        }
    }

    @Override
    public void onDownloadStart(final DownloadInfo downloadInfo) {
        super.onDownloadStart(downloadInfo);
        updateDownloadInfo(downloadInfo);
        addDownloadProgress(downloadInfo, new DownloadProgress(downloadInfo));
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listenerMap.containsKey(downloadInfo)) {
                    DownloadListener downloadListener = listenerMap.get(downloadInfo).get();
                    if (downloadListener != null) {
                        downloadListener.onDownloadStart(downloadInfo);
                    }
                }
            }
        });
    }

    @Override
    public void onDownloadProgress(DownloadInfo downloadInfo) {
        super.onDownloadProgress(downloadInfo);
        updateDownloadProgress(downloadInfo);
        DownloadProgress downloadProgress = progressMap.get(downloadInfo);
        if (downloadProgress != null) {
            if (listenerMap.containsKey(downloadInfo)) {
                downloadProgress.setDownloadListener(listenerMap.get(downloadInfo).get());
            } else {
                downloadProgress.setDownloadListener(null);
            }
            handler.removeCallbacks(downloadProgress);
            handler.post(downloadProgress);
        }
    }

    @Override
    public void onDownloadPause(final DownloadInfo downloadInfo) {
        super.onDownloadPause(downloadInfo);
        updateDownloadInfo(downloadInfo);
        delDownloadProgress(downloadInfo);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listenerMap.containsKey(downloadInfo)) {
                    DownloadListener downloadListener = listenerMap.get(downloadInfo).get();
                    delDownloadListener(downloadInfo);
                    if (downloadListener != null) {
                        downloadListener.onDownloadPause(downloadInfo);
                    }
                }
            }
        });
    }

    @Override
    public void onDownloadCancel(final DownloadInfo downloadInfo) {
        super.onDownloadCancel(downloadInfo);
        deleteDownloadInfo(downloadInfo);
        delDownloadProgress(downloadInfo);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listenerMap.containsKey(downloadInfo)) {
                    DownloadListener downloadListener = listenerMap.get(downloadInfo).get();
                    delDownloadListener(downloadInfo);
                    if (downloadListener != null) {
                        downloadListener.onDownloadCancel(downloadInfo);
                    }
                }
            }
        });
    }

    @Override
    public void onDownloadSuccess(final DownloadInfo downloadInfo) {
        super.onDownloadSuccess(downloadInfo);
        updateDownloadInfo(downloadInfo);
        delDownloadProgress(downloadInfo);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listenerMap.containsKey(downloadInfo)) {
                    DownloadListener downloadListener = listenerMap.get(downloadInfo).get();
                    delDownloadListener(downloadInfo);
                    if (downloadListener != null) {
                        downloadListener.onDownloadSuccess(downloadInfo);
                    }
                }
            }
        });
    }

    @Override
    public void onDownloadFail(final DownloadInfo downloadInfo, final Exception e) {
        super.onDownloadFail(downloadInfo, e);
        updateDownloadInfo(downloadInfo);
        delDownloadProgress(downloadInfo);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listenerMap.containsKey(downloadInfo)) {
                    DownloadListener downloadListener = listenerMap.get(downloadInfo).get();
                    delDownloadListener(downloadInfo);
                    if (downloadListener != null) {
                        downloadListener.onDownloadFail(downloadInfo, e);
                    }
                }
            }
        });
    }

    private void updateDownloadInfo(DownloadInfo downloadInfo) {
        downloadDBHelper.update(downloadInfo);
    }

    private void updateDownloadProgress(DownloadInfo downloadInfo) {
        downloadDBHelper.updateProgress(downloadInfo);
    }

    private void deleteDownloadInfo(DownloadInfo downloadInfo) {
        downloadDBHelper.delete(downloadInfo.getPath());
    }
}