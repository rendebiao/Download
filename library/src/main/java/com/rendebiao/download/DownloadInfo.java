package com.rendebiao.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DB on 2017/8/10.
 */

public class DownloadInfo {

    private int type;
    private long time;
    private String url;
    private String path;
    private String extension;
    private long lastModified;
    private DownloadStatus status;
    private List<DownloadPart> downloadParts;

    DownloadInfo(int type, long time, String url, String path, String extension) {
        this.type = type;
        this.time = time;
        this.url = url;
        this.path = path;
        this.status = DownloadStatus.INIT;
        this.extension = extension;
        downloadParts = new ArrayList<>();
    }

    DownloadInfo(int type, long time, String url, String path, List<DownloadPart> downloadParts, DownloadStatus status, String extension, long lastModified) {
        this.type = type;
        this.time = time;
        this.url = url;
        this.path = path;
        this.downloadParts = downloadParts;
        this.status = status;
        this.extension = extension;
        this.lastModified = lastModified;
        if (this.downloadParts == null) {
            this.downloadParts = new ArrayList<>();
        }
    }

    public int getType() {
        return type;
    }

    void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public String getTempPath() {
        return path + "_temp";
    }

    public long getTime() {
        return time;
    }

    void setTime(long time) {
        this.time = time;
    }

    public int getCurLength() {
        if (downloadParts == null) {
            return 0;
        } else {
            int length = 0;
            for (DownloadPart part : downloadParts) {
                length += part.downloadLength();
            }
            return length;
        }
    }

    public int getTotalLength() {
        if (downloadParts == null) {
            return 0;
        } else {
            int length = 0;
            for (DownloadPart part : downloadParts) {
                length += part.getLength();
            }
            return length;
        }
    }

    public DownloadStatus getStatus() {
        return status;
    }

    void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public String getExtension() {
        return extension;
    }

    void setExtension(String extension) {
        this.extension = extension;
    }

    public long getLastModified() {
        return lastModified;
    }

    void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    void clearDownloadParts() {
        downloadParts.clear();
    }

    void addDownloadPart(DownloadPart downloadPart) {
        downloadParts.add(downloadPart);
    }

    public DownloadPart getDownloadPart(int part) {
        return downloadParts.get(part);
    }

    public List<DownloadPart> getDownloadParts() {
        return downloadParts;
    }

    public boolean isDownloadComplete() {
        boolean complete = downloadParts.size() > 0;
        for (DownloadPart part : downloadParts) {
            complete &= part.isDownloadComplete();
        }
        return complete;
    }

    public void deleteFile() {
        File file = new File(path);
        file.delete();
        file = new File(getTempPath());
        file.delete();
    }

    public String toSimpleString() {
        return "DownloadInfo = " + "type : " + type + " time : " + time + " parts : " + downloadParts.size() + " lastModified : " + lastModified + " curLength : " + getCurLength() + " totalLength : " + getTotalLength();
    }
}
