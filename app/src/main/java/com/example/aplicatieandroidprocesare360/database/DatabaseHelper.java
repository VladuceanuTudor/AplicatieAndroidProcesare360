package com.example.aplicatieandroidprocesare360.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.aplicatieandroidprocesare360.model.Panorama;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "panorama_vr.db";
    private static final int    DB_VERSION = 1;

    // Table: panoramas
    public static final String TABLE_PANORAMAS     = "panoramas";
    public static final String COL_ID              = "id";
    public static final String COL_TITLE           = "title";
    public static final String COL_DESCRIPTION     = "description";
    public static final String COL_FILE_PATH       = "file_path";
    public static final String COL_THUMBNAIL_URL   = "thumbnail_url";
    public static final String COL_UPLOAD_DATE     = "upload_date";
    public static final String COL_LATITUDE        = "latitude";
    public static final String COL_LONGITUDE       = "longitude";
    public static final String COL_STATUS          = "status";
    public static final String COL_JOB_ID          = "job_id";
    public static final String COL_RESULT_URL      = "result_url";
    public static final String COL_DEPTH_MAP_URL   = "depth_map_url";
    public static final String COL_QUALITY_SCORE   = "quality_score";
    public static final String COL_PROC_TIME_MS    = "processing_time_ms";
    public static final String COL_RATING          = "rating";
    public static final String COL_SOURCE_TYPE     = "source_type";
    public static final String COL_MAPILLARY_ID    = "mapillary_id";
    public static final String COL_NOTES           = "notes";

    // Table: processing_log
    public static final String TABLE_LOG           = "processing_log";
    public static final String COL_LOG_ID          = "id";
    public static final String COL_LOG_PANO_ID     = "panorama_id";
    public static final String COL_LOG_TIMESTAMP   = "timestamp";
    public static final String COL_LOG_STEP        = "step_name";
    public static final String COL_LOG_DURATION_MS = "duration_ms";
    public static final String COL_LOG_SUCCESS     = "success";

    private static final String CREATE_PANORAMAS =
        "CREATE TABLE " + TABLE_PANORAMAS + " (" +
            COL_ID            + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_TITLE         + " TEXT, " +
            COL_DESCRIPTION   + " TEXT, " +
            COL_FILE_PATH     + " TEXT, " +
            COL_THUMBNAIL_URL + " TEXT, " +
            COL_UPLOAD_DATE   + " INTEGER, " +
            COL_LATITUDE      + " REAL DEFAULT 0, " +
            COL_LONGITUDE     + " REAL DEFAULT 0, " +
            COL_STATUS        + " TEXT DEFAULT 'PENDING', " +
            COL_JOB_ID        + " TEXT, " +
            COL_RESULT_URL    + " TEXT, " +
            COL_DEPTH_MAP_URL + " TEXT, " +
            COL_QUALITY_SCORE + " REAL DEFAULT 0, " +
            COL_PROC_TIME_MS  + " INTEGER DEFAULT 0, " +
            COL_RATING        + " REAL DEFAULT 0, " +
            COL_SOURCE_TYPE   + " TEXT DEFAULT 'LOCAL', " +
            COL_MAPILLARY_ID  + " TEXT, " +
            COL_NOTES         + " TEXT" +
        ");";

    private static final String CREATE_LOG =
        "CREATE TABLE " + TABLE_LOG + " (" +
            COL_LOG_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_LOG_PANO_ID     + " INTEGER, " +
            COL_LOG_TIMESTAMP   + " INTEGER, " +
            COL_LOG_STEP        + " TEXT, " +
            COL_LOG_DURATION_MS + " INTEGER, " +
            COL_LOG_SUCCESS     + " INTEGER" +
        ");";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PANORAMAS);
        db.execSQL(CREATE_LOG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANORAMAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);
        onCreate(db);
    }

    // ── CRUD panoramas ──────────────────────────────────────────────────

    public long insertPanorama(Panorama p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = toContentValues(p);
        return db.insert(TABLE_PANORAMAS, null, cv);
    }

    public int updatePanorama(Panorama p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = toContentValues(p);
        return db.update(TABLE_PANORAMAS, cv, COL_ID + "=?",
                new String[]{String.valueOf(p.getId())});
    }

    public int deletePanorama(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LOG, COL_LOG_PANO_ID + "=?", new String[]{String.valueOf(id)});
        return db.delete(TABLE_PANORAMAS, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Panorama getPanoramaById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PANORAMAS, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Panorama p = null;
        if (c.moveToFirst()) p = fromCursor(c);
        c.close();
        return p;
    }

    public List<Panorama> getAllPanoramas() {
        return queryPanoramas(null, null);
    }

    public List<Panorama> getPanoramasByStatus(String status) {
        return queryPanoramas(COL_STATUS + "=?", new String[]{status});
    }

    public List<Panorama> getPanoramasByDateRange(long from, long to) {
        return queryPanoramas(COL_UPLOAD_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(from), String.valueOf(to)});
    }

    private List<Panorama> queryPanoramas(String selection, String[] args) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PANORAMAS, null, selection, args,
                null, null, COL_UPLOAD_DATE + " DESC");
        List<Panorama> list = new ArrayList<>();
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public void updateStatus(int id, String status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_STATUS, status);
        db.update(TABLE_PANORAMAS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void updateJobResult(int id, String jobId, String resultUrl,
                                String depthMapUrl, float qualityScore, long procTimeMs) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_JOB_ID, jobId);
        cv.put(COL_RESULT_URL, resultUrl);
        cv.put(COL_DEPTH_MAP_URL, depthMapUrl);
        cv.put(COL_QUALITY_SCORE, qualityScore);
        cv.put(COL_PROC_TIME_MS, procTimeMs);
        cv.put(COL_STATUS, Panorama.STATUS_DONE);
        db.update(TABLE_PANORAMAS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void updateRating(int id, float rating) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_RATING, rating);
        db.update(TABLE_PANORAMAS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // ── Stats helpers ───────────────────────────────────────────────────

    public int countAll()  { return countWhere(null, null); }
    public int countDone() { return countWhere(COL_STATUS + "=?", new String[]{Panorama.STATUS_DONE}); }
    public int countFailed() { return countWhere(COL_STATUS + "=?", new String[]{Panorama.STATUS_FAILED}); }
    public int countPending() { return countWhere(COL_STATUS + " IN (?,?)",
            new String[]{Panorama.STATUS_PENDING, Panorama.STATUS_PROCESSING}); }

    private int countWhere(String where, String[] args) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PANORAMAS, new String[]{"COUNT(*)"},
                where, args, null, null, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public double getAverageProcessingTime() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT AVG(" + COL_PROC_TIME_MS + ") FROM " +
                TABLE_PANORAMAS + " WHERE " + COL_STATUS + "=?",
                new String[]{Panorama.STATUS_DONE});
        double avg = 0;
        if (c.moveToFirst()) avg = c.getDouble(0);
        c.close();
        return avg;
    }

    public double getAverageQualityScore() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT AVG(" + COL_QUALITY_SCORE + ") FROM " +
                TABLE_PANORAMAS + " WHERE " + COL_STATUS + "=?",
                new String[]{Panorama.STATUS_DONE});
        double avg = 0;
        if (c.moveToFirst()) avg = c.getDouble(0);
        c.close();
        return avg;
    }

    // ── Log ─────────────────────────────────────────────────────────────

    public void insertLog(int panoramaId, String step, long durationMs, boolean success) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LOG_PANO_ID, panoramaId);
        cv.put(COL_LOG_TIMESTAMP, System.currentTimeMillis());
        cv.put(COL_LOG_STEP, step);
        cv.put(COL_LOG_DURATION_MS, durationMs);
        cv.put(COL_LOG_SUCCESS, success ? 1 : 0);
        db.insert(TABLE_LOG, null, cv);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ContentValues toContentValues(Panorama p) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE,         p.getTitle());
        cv.put(COL_DESCRIPTION,   p.getDescription());
        cv.put(COL_FILE_PATH,     p.getFilePath());
        cv.put(COL_THUMBNAIL_URL, p.getThumbnailUrl());
        cv.put(COL_UPLOAD_DATE,   p.getUploadDate());
        cv.put(COL_LATITUDE,      p.getLatitude());
        cv.put(COL_LONGITUDE,     p.getLongitude());
        cv.put(COL_STATUS,        p.getStatus());
        cv.put(COL_JOB_ID,        p.getJobId());
        cv.put(COL_RESULT_URL,    p.getResultUrl());
        cv.put(COL_DEPTH_MAP_URL, p.getDepthMapUrl());
        cv.put(COL_QUALITY_SCORE, p.getQualityScore());
        cv.put(COL_PROC_TIME_MS,  p.getProcessingTimeMs());
        cv.put(COL_RATING,        p.getRating());
        cv.put(COL_SOURCE_TYPE,   p.getSourceType());
        cv.put(COL_MAPILLARY_ID,  p.getMapillaryId());
        cv.put(COL_NOTES,         p.getNotes());
        return cv;
    }

    private Panorama fromCursor(Cursor c) {
        Panorama p = new Panorama();
        p.setId(            c.getInt(   c.getColumnIndexOrThrow(COL_ID)));
        p.setTitle(         c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
        p.setDescription(   c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)));
        p.setFilePath(      c.getString(c.getColumnIndexOrThrow(COL_FILE_PATH)));
        p.setThumbnailUrl(  c.getString(c.getColumnIndexOrThrow(COL_THUMBNAIL_URL)));
        p.setUploadDate(    c.getLong(  c.getColumnIndexOrThrow(COL_UPLOAD_DATE)));
        p.setLatitude(      c.getDouble(c.getColumnIndexOrThrow(COL_LATITUDE)));
        p.setLongitude(     c.getDouble(c.getColumnIndexOrThrow(COL_LONGITUDE)));
        p.setStatus(        c.getString(c.getColumnIndexOrThrow(COL_STATUS)));
        p.setJobId(         c.getString(c.getColumnIndexOrThrow(COL_JOB_ID)));
        p.setResultUrl(     c.getString(c.getColumnIndexOrThrow(COL_RESULT_URL)));
        p.setDepthMapUrl(   c.getString(c.getColumnIndexOrThrow(COL_DEPTH_MAP_URL)));
        p.setQualityScore(  c.getFloat( c.getColumnIndexOrThrow(COL_QUALITY_SCORE)));
        p.setProcessingTimeMs(c.getLong(c.getColumnIndexOrThrow(COL_PROC_TIME_MS)));
        p.setRating(        c.getFloat( c.getColumnIndexOrThrow(COL_RATING)));
        p.setSourceType(    c.getString(c.getColumnIndexOrThrow(COL_SOURCE_TYPE)));
        p.setMapillaryId(   c.getString(c.getColumnIndexOrThrow(COL_MAPILLARY_ID)));
        p.setNotes(         c.getString(c.getColumnIndexOrThrow(COL_NOTES)));
        return p;
    }
}
