package com.rdb.download;

/**
 * Created by DB on 2017/8/11 0011.
 */

class DownloadPart {

    private int order;
    private int startPosition;
    private int endPosition;
    private int curPosition;

    DownloadPart(int order, int startPosition, int endPosition) {
        this.order = order;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.curPosition = startPosition;
    }

    public DownloadPart(int order, int startPosition, int endPosition, int curPosition) {
        this.order = order;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.curPosition = curPosition;
    }

    public int getOrder() {
        return order;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public int getCurPosition() {
        return curPosition;
    }

    public int getLength() {
        return endPosition - startPosition + 1;
    }

    void addLength(int length) {
        this.curPosition += length;
    }

    public int downloadLength() {
        return curPosition - startPosition;
    }

    public boolean isDownloadComplete() {
        return endPosition > 0 && curPosition == endPosition + 1;
    }
}
