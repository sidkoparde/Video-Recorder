package com.danielkim.soundrecorder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.danielkim.soundrecorder.FolderItem;
import com.danielkim.soundrecorder.RecordingItem;
import com.danielkim.soundrecorder.listeners.OnDatabaseChangedListener;

import java.util.Comparator;

/**
 * Created by Daniel on 12/29/2014.
 */
public class DBHelper extends SQLiteOpenHelper {
    private Context mContext;

    private static final String LOG_TAG = "DBHelper";

    private static OnDatabaseChangedListener mOnDatabaseChangedListener;

    public static final String DATABASE_NAME = "saved_recordings.db";
    private static final int DATABASE_VERSION = 1;

    public static abstract class DBHelperItem implements BaseColumns {
        public static final String TABLE_RECORDING_NAME = "saved_recordings";

        public static final String COLUMN_NAME_RECORDING_NAME = "recording_name";
        public static final String COLUMN_NAME_RECORDING_FILE_PATH = "file_path";
        public static final String COLUMN_NAME_RECORDING_LENGTH = "length";
        public static final String COLUMN_NAME_TIME_ADDED = "time_added";
        public static final String COLUMN_NAME_FOLDER = "folder_id";
        public static final String COLUMN_NAME_ISAUDIO = "isAudio";
        public static final String COLUMN_NAME_NOTE = "note";
    }

    public static abstract class DBHelperFolder implements BaseColumns {
        public static final String TABLE_FOLDER_NAME = "saved_folders";

        //public static final String COLUMN_NAME_FOLDER_ID = "_id";
        public static final String COLUMN_NAME_FOLDER_NAME = "folder_name";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_RECORDING_ENTRIES =
            "CREATE TABLE " + DBHelperItem.TABLE_RECORDING_NAME + " (" +
                    DBHelperItem._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_NAME + TEXT_TYPE + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH + TEXT_TYPE + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_LENGTH + " INTEGER " + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_TIME_ADDED + " INTEGER " + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_FOLDER + " INTEGER " + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_NOTE + TEXT_TYPE + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_ISAUDIO + " INTEGER " + ")";

    //for folder
    private static final String SQL_CREATE_FOLDER_ENTRIES =
            "CREATE TABLE " + DBHelperFolder.TABLE_FOLDER_NAME + " (" +
                    DBHelperFolder._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    DBHelperFolder.COLUMN_NAME_FOLDER_NAME + TEXT_TYPE + ")";

    @SuppressWarnings("unused")
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + DBHelperItem.TABLE_RECORDING_NAME;

    private static final String SQL_CREATE_DEFAULT_FOLDER = "INSERT INTO " + DBHelperFolder.TABLE_FOLDER_NAME + " VALUES (NULL, \"All\")";

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_RECORDING_ENTRIES);
        db.execSQL(SQL_CREATE_FOLDER_ENTRIES);

        db.execSQL(SQL_CREATE_DEFAULT_FOLDER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    public static void setOnDatabaseChangedListener(OnDatabaseChangedListener listener) {
        mOnDatabaseChangedListener = listener;
    }

    public RecordingItem getItemAt(int position) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                DBHelperItem._ID,
                DBHelperItem.COLUMN_NAME_RECORDING_NAME,
                DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH,
                DBHelperItem.COLUMN_NAME_RECORDING_LENGTH,
                DBHelperItem.COLUMN_NAME_TIME_ADDED,
                DBHelperItem.COLUMN_NAME_FOLDER,
                DBHelperItem.COLUMN_NAME_NOTE,
                DBHelperItem.COLUMN_NAME_ISAUDIO
        };
        Cursor c = db.query(DBHelperItem.TABLE_RECORDING_NAME, projection, null, null, null, null, null);
        if (c.moveToPosition(position)) {
            RecordingItem item = new RecordingItem();
            item.setId(c.getInt(c.getColumnIndex(DBHelperItem._ID)));
            item.setName(c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_NAME)));
            item.setFilePath(c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH)));
            item.setLength(c.getInt(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_LENGTH)));
            item.setTime(c.getLong(c.getColumnIndex(DBHelperItem.COLUMN_NAME_TIME_ADDED)));
            item.setFolderID(c.getInt(c.getColumnIndex(DBHelperItem.COLUMN_NAME_FOLDER)));
            item.setNote(c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_NOTE)));
            c.close();
            return item;
        }
        return null;
    }

    public void removeItemWithId(int id) {
        SQLiteDatabase db = getWritableDatabase();
        String[] whereArgs = { String.valueOf(id) };
        db.delete(DBHelperItem.TABLE_RECORDING_NAME, "_ID=?", whereArgs);
    }

    public int getCount() {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = { DBHelperItem._ID };
        Cursor c = db.query(DBHelperItem.TABLE_RECORDING_NAME, projection, null, null, null, null, null);
        int count = c.getCount();
        c.close();
        return count;
    }

    public Context getContext() {
        return mContext;
    }

}
