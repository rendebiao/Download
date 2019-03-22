package com.rdb.download;

class DownloadData {

    private int offset;
    private int length;
    private byte[] bytes;
    private DownloadPart downloadPart;

    public DownloadData(int length) {
        this.bytes = new byte[length];
    }

    public DownloadPart getDownloadPart() {
        return downloadPart;
    }

    public void setDownloadPart(DownloadPart downloadPart) {
        this.downloadPart = downloadPart;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void recycle() {
        bytes = null;
        downloadPart = null;
    }
}
