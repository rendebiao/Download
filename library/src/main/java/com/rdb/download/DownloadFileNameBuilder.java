package com.rdb.download;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by DB on 2017/8/10.
 */

public abstract class DownloadFileNameBuilder {

    public static DownloadFileNameBuilder DATE = new DownloadFileNameBuilder() {

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        @Override
        public synchronized String buildName(String urlString) {
            return DATE_FORMAT.format(new Date(System.currentTimeMillis()));
        }
    };
    public static DownloadFileNameBuilder DATE_WITH_SUFFIX = new DownloadFileNameBuilder() {

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        @Override
        public synchronized String buildName(String urlString) {
            return DATE_FORMAT.format(new Date(System.currentTimeMillis())) + nameFromUrl(urlString)[2];
        }
    };
    public static DownloadFileNameBuilder ORIGINAL = new DownloadFileNameBuilder() {
        @Override
        public String buildName(String urlString) {
            return nameFromUrl(urlString)[1];
        }
    };
    public static DownloadFileNameBuilder ORIGINAL_WITH_SUFFIX = new DownloadFileNameBuilder() {
        @Override
        public String buildName(String urlString) {
            return nameFromUrl(urlString)[0];
        }
    };
    public static DownloadFileNameBuilder HASHCODE = new DownloadFileNameBuilder() {
        @Override
        public String buildName(String urlString) {
            return String.valueOf(urlString.hashCode());
        }
    };
    public static DownloadFileNameBuilder HASHCODE_WITH_SUFFIX = new DownloadFileNameBuilder() {
        @Override
        public String buildName(String urlString) {
            return urlString.hashCode() + nameFromUrl(urlString)[2];
        }
    };
    public static DownloadFileNameBuilder MILLISECOND = new DownloadFileNameBuilder() {
        @Override
        public String buildName(String urlString) {
            return String.valueOf(System.currentTimeMillis());
        }
    };
    public static DownloadFileNameBuilder MILLISECOND_WITH_SUFFIX = new DownloadFileNameBuilder() {
        @Override
        public String buildName(String urlString) {
            return System.currentTimeMillis() + nameFromUrl(urlString)[2];
        }
    };

    public abstract String buildName(String urlString);

    public String[] nameFromUrl(String urlString) {
        String[] results = new String[3];
        if (!TextUtils.isEmpty(urlString)) {
            String[] paths;
            if (urlString.contains("?")) {
                paths = urlString.split("\\?")[0].split("/");
            } else {
                paths = urlString.split("/");
            }
            results[0] = paths[paths.length - 1];
            int index = results[0].lastIndexOf(".");
            if (index > 0 && index < results[0].length() - 1) {
                results[1] = results[0].substring(0, index);
                results[2] = results[0].substring(index);
            } else {
                results[1] = results[0];
                results[2] = "";
            }
        }
        return results;
    }
}
