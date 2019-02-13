package com.rdb.download;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * Created by DB on 2017/8/13 0013.
 */

public class DownloadConfig {

    private static final String LOG_TAG = "Download";
    private static boolean logEnable = true;
    private final int maxTaskCount;
    private final int bufferSize;
    private final String databasePath;
    private final DownloadPartRule downloadPartRule;

    public DownloadConfig(Builder builder) {
        this.bufferSize = builder.bufferSize;
        this.maxTaskCount = builder.maxTaskCount;
        this.databasePath = builder.databasePath;
        this.downloadPartRule = builder.downloadPartRule;
    }

    public static void log(String log) {
        if (logEnable) {
            Log.e(LOG_TAG, log);
        }
    }

    public static void setLogEnable(boolean logEnable) {
        DownloadConfig.logEnable = logEnable;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getMaxTaskCount() {
        return maxTaskCount;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public DownloadPartRule getDownloadPartRule() {
        return downloadPartRule;
    }

    public static class Builder {

        private int bufferSize = 102400;
        private int maxTaskCount = 2;
        private String databasePath = "download";
        private DownloadPartRule downloadPartRule = new DownloadPartRule() {
            @Override
            public int getPartCounts(int length) {
                if (length < 50 * 1024 * 1024) {
                    return 1;
                } else if (length < 100 * 1024 * 1024) {
                    return 2;
                } else if (length < 500 * 1024 * 1024) {
                    return 3;
                } else {
                    return 4;
                }
            }
        };

        public Builder setMaxTaskCount(int maxTaskCount) {
            if (maxTaskCount > 0 && maxTaskCount <= 8) {
                this.maxTaskCount = maxTaskCount;
            }
            return this;
        }

        public Builder setBufferSize(int bufferSize) {
            if (bufferSize > 1024 && bufferSize < 1024 * 1024) {
                this.bufferSize = bufferSize;
            }
            return this;
        }

        public Builder setDatabasePath(String databasePath) {
            if (!TextUtils.isEmpty(databasePath)) {
                if (databasePath.contains("/")) {
                    File file = new File(databasePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    if (file.getParentFile().exists()) {
                        this.databasePath = databasePath;
                    }
                } else {
                    this.databasePath = databasePath;
                }
            }
            return this;
        }

        public Builder setDownloadPartRule(DownloadPartRule downloadPartRule) {
            if (downloadPartRule != null) {
                this.downloadPartRule = downloadPartRule;
            }
            return this;
        }

        public DownloadConfig build() {
            return new DownloadConfig(this);
        }
    }
}
