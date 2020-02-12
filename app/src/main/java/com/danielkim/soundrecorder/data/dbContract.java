package com.danielkim.soundrecorder.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by sid on 3/22/18.
 */

public final class dbContract {

    private dbContract() {}

    public static final String CONTENT_AUTHORITY = "com.danielkim.soundrecorder";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_FOLDERS = "folders";

    public static final String PATH_FILES = "files";

    public static final class RecordingsEntry implements BaseColumns {

        public final static String TABLE_NAME = "saved_recordings";

        public final static String _ID = BaseColumns._ID;
    }

    public static final class FoldersEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FOLDERS);

        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FOLDERS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FOLDERS;

        public static final String TABLE_NAME = "saved_folders";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_FOLDER_NAME = "folder_name";
    }

    public static final class FilesEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FILES);

        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FILES;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FILES;

        public static final String TABLE_NAME = "saved_recordings";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_RECORDING_NAME = "recording_name";
        public static final String COLUMN_RECORDING_FILE_PATH = "file_path";
        public static final String COLUMN_RECORDING_LENGTH = "length";
        public static final String COLUMN_TIME_ADDED = "time_added";
        public static final String COLUMN_FOLDER_ID = "folder_id";
        public static final String COLUMN_ISAUDIO = "isAudio";
        public static final String COLUMN_NOTE = "note";
    }
}
