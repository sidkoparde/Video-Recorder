package com.danielkim.soundrecorder.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.danielkim.soundrecorder.data.dbContract.FoldersEntry;
import com.danielkim.soundrecorder.data.dbContract.FilesEntry;

/**
 * Created by sid on 3/22/18.
 */

public class dbProvider extends ContentProvider {

    public static final String LOG_TAG = dbProvider.class.getSimpleName();

    private static final int FOLDERS = 100;

    private static final int FOLDER_ID = 101;

    private static final int FILES = 200;

    private static final int FILE_ID = 201;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(dbContract.CONTENT_AUTHORITY, dbContract.PATH_FOLDERS, FOLDERS);;
        sUriMatcher.addURI(dbContract.CONTENT_AUTHORITY, dbContract.PATH_FOLDERS + "/#", FOLDER_ID);

        sUriMatcher.addURI(dbContract.CONTENT_AUTHORITY, dbContract.PATH_FILES, FILES);
        sUriMatcher.addURI(dbContract.CONTENT_AUTHORITY, dbContract.PATH_FILES + "/#", FILE_ID);

    }

    private DBHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DBHelper(getContext());

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case FOLDERS:
                cursor = database.query(FoldersEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case FOLDER_ID:
                selection = FoldersEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(FoldersEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case FILES:
                cursor = database.query(FilesEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case FILE_ID:
                selection = FilesEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(FilesEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI" + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {

            case FOLDER_ID:
                return FoldersEntry.CONTENT_ITEM_TYPE;
            case FOLDERS:
                return FoldersEntry.CONTENT_LIST_TYPE;
            case FILE_ID:
                return FilesEntry.CONTENT_ITEM_TYPE;
            case FILES:
                return FilesEntry.CONTENT_LIST_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case FOLDERS:
                return insertFolder(uri, contentValues);
            case FILES:
                return insertFile(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertFile(Uri uri, ContentValues values) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long id = db.insert(FilesEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }

    private Uri insertFolder(Uri uri, ContentValues values) {
        String name = values.getAsString(FoldersEntry.COLUMN_FOLDER_NAME);

        if (name == null) {
            throw new IllegalArgumentException("Folder requires a name");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(FoldersEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FOLDERS:
                rowsDeleted = database.delete(FoldersEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FOLDER_ID:
                selection = FoldersEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(FoldersEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FILE_ID:
                selection = FilesEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(FilesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FOLDERS:
                return updateFolder(uri, contentValues, selection, selectionArgs);
            case FOLDER_ID:
                selection = FoldersEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updateFolder(uri, contentValues, selection, selectionArgs);
            case FILE_ID:
                selection = FilesEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updateFile(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateFolder(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(FoldersEntry.COLUMN_FOLDER_NAME)) {
            String name = values.getAsString(FoldersEntry.COLUMN_FOLDER_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Folder requires a name");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(FoldersEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    private int updateFile(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(FilesEntry.COLUMN_RECORDING_NAME)) {
            String name = values.getAsString(FilesEntry.COLUMN_RECORDING_NAME);
            if (name == null) {
                throw new IllegalArgumentException("File requires a name");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(FilesEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }
}
