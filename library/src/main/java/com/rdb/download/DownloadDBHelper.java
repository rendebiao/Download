package com.rdb.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class DownloadDBHelper extends SQLiteOpenHelper {

    public static final String TABLE_INFO = "download_info";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_EXTENSION = "extension";
    public static final String COLUMN_LAST_MODIFIED = "lastModified";

    public static final String TABLE_PART = "download_part";
    public static final String COLUMN_PART_ORDER = "orderValue";
    public static final String COLUMN_PART_START = "startPosition";
    public static final String COLUMN_PART_END = "endPosition";
    public static final String COLUMN_PART_CUR = "curPosition";

    private SQLiteDatabase dataBase;
    private AtomicInteger atomicInteger = new AtomicInteger();
    private Map<String, Integer> cacheLengths = new HashMap<>();
    private Map<String, DownloadInfo> cacheInfos = new HashMap<>();
    private DownloadComparator comparator = new DownloadComparator();

    public DownloadDBHelper(Context context, String name) {
        super(context, name, null, 1);
        SQLiteDatabase dataBase = openDatabase();
        Map<String, List<DownloadPart>> downloadParts = new HashMap<>();
        Cursor cursor = dataBase.query(TABLE_PART, null, "1 = 1", new String[]{}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(COLUMN_PATH));
                List<DownloadPart> parts = downloadParts.get(path);
                if (parts == null) {
                    parts = new ArrayList<>();
                    downloadParts.put(path, parts);
                }
                parts.add(cursorToDownloadPart(cursor));
            }
            cursor.close();
        }
        cursor = dataBase.query(TABLE_INFO, null, "1 = 1", new String[]{}, null, null, COLUMN_TIME);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo downloadInfo = cursorToDownloadInfo(cursor, downloadParts);
                cacheInfos.put(downloadInfo.getPath(), downloadInfo);
                cacheLengths.put(downloadInfo.getPath(), downloadInfo.getCurLength());
            }
            cursor.close();
        }
        closeDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        DownloadConfig.log("DownloadDBHelper onCreate");
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_INFO + " ("
                + COLUMN_TYPE + " INTEGER NOT NULL, "
                + COLUMN_PATH + " NVARCHAR NOT NULL, "
                + COLUMN_URL + " NVARCHAR NOT NULL, "
                + COLUMN_TIME + " LONG NOT NULL, "
                + COLUMN_LAST_MODIFIED + " LONG NOT NULL, "
                + COLUMN_STATUS + " INTEGER NOT NULL, "
                + COLUMN_EXTENSION + " TEXT, PRIMARY KEY ("
                + COLUMN_PATH + "))";
        db.execSQL(sql);
        sql = "CREATE TABLE IF NOT EXISTS " + TABLE_PART + " ("
                + COLUMN_PATH + " NVARCHAR NOT NULL, "
                + COLUMN_PART_ORDER + " INTEGER NOT NULL, "
                + COLUMN_PART_START + " INTEGER NOT NULL, "
                + COLUMN_PART_END + " INTEGER NOT NULL, "
                + COLUMN_PART_CUR + " INTEGER NOT NULL, PRIMARY KEY ("
                + COLUMN_PATH + ", " + COLUMN_PART_ORDER + "))";
        db.execSQL(sql);
    }

    public synchronized SQLiteDatabase openDatabase() {
        if (dataBase != null && !dataBase.isOpen()) {
            dataBase = getWritableDatabase();
        } else if (atomicInteger.incrementAndGet() == 1) {
            dataBase = getWritableDatabase();
        }
        return dataBase;
    }

    public synchronized void closeDatabase() {
        if (atomicInteger.decrementAndGet() == 0) {
            dataBase.close();
            dataBase = null;
        }
    }

    @Override
    public final void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    protected final boolean update(DownloadInfo downloadInfo) {
        if (downloadInfo == null) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        SQLiteDatabase dataBase = openDatabase();
        dataBase.beginTransaction();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TYPE, downloadInfo.getType());
        contentValues.put(COLUMN_PATH, downloadInfo.getPath());
        contentValues.put(COLUMN_URL, downloadInfo.getUrl());
        contentValues.put(COLUMN_TIME, downloadInfo.getTime());
        contentValues.put(COLUMN_STATUS, downloadInfo.getStatus().getStatus());
        contentValues.put(COLUMN_EXTENSION, downloadInfo.getExtension());
        contentValues.put(COLUMN_LAST_MODIFIED, downloadInfo.getLastModified());
        boolean replace = dataBase.replace(TABLE_INFO, null, contentValues) >= 0;
        if (replace) {
            for (int i = 0; i < downloadInfo.getDownloadParts().size(); i++) {
                contentValues = new ContentValues();
                DownloadPart downloadPart = downloadInfo.getDownloadParts().get(i);
                contentValues.put(COLUMN_PATH, downloadInfo.getPath());
                contentValues.put(COLUMN_PART_ORDER, downloadPart.getOrder());
                contentValues.put(COLUMN_PART_START, downloadPart.getStartPosition());
                contentValues.put(COLUMN_PART_END, downloadPart.getEndPosition());
                contentValues.put(COLUMN_PART_CUR, downloadPart.getCurPosition());
                replace = replace && dataBase.replace(TABLE_PART, null, contentValues) >= 0;
                if (!replace) {
                    break;
                }
            }
        }
        if (replace) {
            cacheInfos.put(downloadInfo.getPath(), downloadInfo);
            dataBase.setTransactionSuccessful();
        }
        dataBase.endTransaction();
        closeDatabase();
        DownloadConfig.log("DownloadDBHelper update : " + downloadInfo.getFileName() + " " + downloadInfo.getStatus() + " " + downloadInfo.getCurLength() + " time = " + (System.currentTimeMillis() - startTime));
        return replace;
    }

    protected final boolean updateProgress(DownloadInfo downloadInfo) {
        if (downloadInfo == null || downloadInfo.getCurLength() == 0) {
            return false;
        }
        if (cacheLengths.containsKey(downloadInfo.getPath())) {
            int length = cacheLengths.get(downloadInfo.getPath());
            if (length == downloadInfo.getCurLength()) {
                return false;
            }
        }
        long startTime = System.currentTimeMillis();
        SQLiteDatabase dataBase = openDatabase();
        dataBase.beginTransaction();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_LAST_MODIFIED, downloadInfo.getLastModified());
        boolean update = dataBase.update(TABLE_INFO, contentValues, COLUMN_PATH + " = ?", new String[]{downloadInfo.getPath()}) >= 0;
        if (update) {
            for (int i = 0; i < downloadInfo.getDownloadParts().size(); i++) {
                contentValues = new ContentValues();
                DownloadPart downloadPart = downloadInfo.getDownloadParts().get(i);
                contentValues.put(COLUMN_PART_CUR, downloadPart.getCurPosition());
                update = update && dataBase.update(TABLE_PART, contentValues, COLUMN_PATH + " = ? and " + COLUMN_PART_ORDER + " = ?", new String[]{downloadInfo.getPath(), String.valueOf(downloadPart.getOrder())}) >= 0;
                if (!update) {
                    break;
                }
            }
            if (update) {
                cacheInfos.put(downloadInfo.getPath(), downloadInfo);
                cacheLengths.put(downloadInfo.getPath(), downloadInfo.getCurLength());
                dataBase.setTransactionSuccessful();
            }
        }
        dataBase.endTransaction();
        closeDatabase();
        DownloadConfig.log("DownloadDBHelper updateProgress : " + downloadInfo.getFileName() + " " + downloadInfo.getCurLength() + "/" + downloadInfo.getTotalLength() + " time = " + (System.currentTimeMillis() - startTime) + "  " + update);
        return update;
    }

    protected final List<DownloadInfo> getDownloadInfos(boolean desc) {
        List<DownloadInfo> downloadInfos = new ArrayList<>(cacheInfos.values());
        comparator.setDesc(desc);
        Collections.sort(downloadInfos, comparator);
        return downloadInfos;
    }

    private DownloadInfo cursorToDownloadInfo(Cursor cursor, Map<String, List<DownloadPart>> downloadParts) {
        int type = cursor.getInt(cursor.getColumnIndex(COLUMN_TYPE));
        String url = cursor.getString(cursor.getColumnIndex(COLUMN_URL));
        String path = cursor.getString(cursor.getColumnIndex(COLUMN_PATH));
        String extension = cursor.getString(cursor.getColumnIndex(COLUMN_EXTENSION));
        long time = cursor.getLong(cursor.getColumnIndex(COLUMN_TIME));
        int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
        long lastModified = cursor.getLong(cursor.getColumnIndex(COLUMN_LAST_MODIFIED));
        DownloadInfo downloadInfo = new DownloadInfo(type, time, url, path, downloadParts.get(path), DownloadStatus.fromStatus(status), extension, lastModified);
        if (downloadInfo.getStatus() != DownloadStatus.SUCCESS && downloadInfo.isDownloadComplete()) {
            downloadInfo.setStatus(DownloadStatus.SUCCESS);
            update(downloadInfo);
        } else {
            if (downloadInfo.getStatus() == DownloadStatus.RUNNING) {
                downloadInfo.setStatus(DownloadStatus.PAUSE);
                update(downloadInfo);
            }
        }
        return downloadInfo;
    }

    private DownloadPart cursorToDownloadPart(Cursor cursor) {
        int order = cursor.getInt(cursor.getColumnIndex(COLUMN_PART_ORDER));
        int start = cursor.getInt(cursor.getColumnIndex(COLUMN_PART_START));
        int end = cursor.getInt(cursor.getColumnIndex(COLUMN_PART_END));
        int cur = cursor.getInt(cursor.getColumnIndex(COLUMN_PART_CUR));
        return new DownloadPart(order, start, end, cur);
    }

    protected final boolean delete(String path) {
        if (path == null) {
            DownloadConfig.log("DownloadDBHelper delete error : path == null");
        } else {
            boolean delete = deleteByCondition(TABLE_INFO, COLUMN_PATH + " = ?", new String[]{
                    path
            }) && deleteByCondition(TABLE_PART, COLUMN_PATH + " = ?", new String[]{
                    path
            });
            if (delete) {
                cacheInfos.remove(path);
                cacheLengths.remove(path);
            }
            return delete;
        }
        return false;
    }

    protected final boolean deleteAll() {
        return deleteByCondition(TABLE_INFO, "1 = 1", new String[]{
        }) && deleteByCondition(TABLE_PART, "1 = 1", new String[]{
        });
    }

    private boolean deleteByCondition(String table, String whereClause, String[] whereArgs) {
        SQLiteDatabase dataBase = openDatabase();
        boolean delete = dataBase.delete(table, whereClause, whereArgs) >= 0;
        closeDatabase();
        return delete;
    }
}
