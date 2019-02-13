package com.rdb.download;

import java.util.Comparator;

/**
 * Created by DB on 2018/6/6.
 */

public class DownloadComparator implements Comparator<DownloadInfo> {

    private boolean desc;

    public void setDesc(boolean desc) {
        this.desc = desc;
    }

    @Override
    public int compare(DownloadInfo o1, DownloadInfo o2) {
        if (o1.getTime() == o2.getTime()) {
            return 0;
        } else if (desc) {
            return o1.getTime() > o2.getTime() ? -1 : 1;
        }
        return o1.getTime() < o2.getTime() ? -1 : 1;
    }
}
