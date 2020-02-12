
package com.danielkim.soundrecorder.activities;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.database.Cursor;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.danielkim.soundrecorder.R;
import com.danielkim.soundrecorder.RecordingItem;
import com.danielkim.soundrecorder.adapters.FileCursorAdapter;
import com.danielkim.soundrecorder.data.dbContract.FilesEntry;
import com.danielkim.soundrecorder.data.dbContract.FoldersEntry;
import com.danielkim.soundrecorder.fragments.PlaybackFragment;
import java.io.File;
import java.util.ArrayList;

public class FileListActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = FileListActivity.class.getSimpleName();

    private static final int FILE_LOADER = 0;

    private Uri mCurrentFolderUri;

    private FileCursorAdapter mFileCursorAdapter;

    private Context mContext;

    private ActionBar mActionBar;

    private RecordingItem recordingItem;
    private int mFolderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        mContext = getApplicationContext();

        ListView mListView = (ListView) findViewById(R.id.listView);

        Intent intent = getIntent();
        mCurrentFolderUri = intent.getData();

        recordingItem = new RecordingItem();
        mFolderId = recordingItem.getFolderID();

        String[] projection = new String[] {
                FoldersEntry.COLUMN_FOLDER_NAME
        };
        Cursor c = getContentResolver().query(mCurrentFolderUri, projection, null, null, null);

        c.moveToFirst();

        int folderNameColIndex = c.getColumnIndex(FoldersEntry.COLUMN_FOLDER_NAME);

        String folderName = c.getString(folderNameColIndex);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();

        if (mActionBar != null) {
            mActionBar.setTitle(folderName);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowHomeEnabled(true);
        }

        mFileCursorAdapter = new FileCursorAdapter(mContext, null);

        mListView.setAdapter(mFileCursorAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                Uri currentFileUri = ContentUris.withAppendedId(FilesEntry.CONTENT_URI, id);

                try {
                    PlaybackFragment playbackFragment =
                            new PlaybackFragment().newInstance(currentFileUri);

                    FragmentTransaction transaction = getSupportFragmentManager()
                            .beginTransaction();

                    playbackFragment.show(transaction, "dialog_playback");

                } catch (Exception e) {
                    Log.e(LOG_TAG, "exception", e);
                }
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                ArrayList<String> entrys = new ArrayList<String>();
                entrys.add(mContext.getString(R.string.dialog_file_note));
                entrys.add(mContext.getString(R.string.dialog_file_share));
                entrys.add(mContext.getString(R.string.dialog_file_rename));
                entrys.add(mContext.getString(R.string.dialog_file_move));
                entrys.add(mContext.getString(R.string.dialog_file_delete));

                final Uri currentFileUri = ContentUris.withAppendedId(FilesEntry.CONTENT_URI, id);

                final CharSequence[] items = entrys.toArray(new CharSequence[entrys.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(FileListActivity.this);
                builder.setTitle(mContext.getString(R.string.dialog_title_options));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            addNoteDialog(currentFileUri);
                        } if (item == 1) {
                            shareFileDialog(currentFileUri);
                        } else if (item == 2) {
                            renameFileDialog(currentFileUri);
                        } else if (item == 3) {
                            moveFileDialog(currentFileUri);
                        } else if (item == 4) {
                            deleteFileDialog(currentFileUri);
                        }
                    }
                });
                builder.setCancelable(true);
                builder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();

                return true;
            }
        });

        getLoaderManager().initLoader(FILE_LOADER, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        long id = ContentUris.parseId(mCurrentFolderUri);

        String[] projection = new String[] {
                FilesEntry._ID,
                FilesEntry.COLUMN_RECORDING_NAME,
                FilesEntry.COLUMN_ISAUDIO,
                FilesEntry.COLUMN_RECORDING_LENGTH,
                FilesEntry.COLUMN_NOTE,
                FilesEntry.COLUMN_TIME_ADDED
        };

        String selection = null;

        String selectionArgs[] = null;

        if (id != 1) {

            selection = FilesEntry.COLUMN_FOLDER_ID + "=?";

            selectionArgs = new String[] {String.valueOf(id)};
        }


        return new CursorLoader(this, FilesEntry.CONTENT_URI, projection, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mFileCursorAdapter.swapCursor(cursor);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFileCursorAdapter.swapCursor(null);
    }

    private void shareFileDialog(final Uri currentFileUri) {

        String[] projection = {
                FilesEntry.COLUMN_RECORDING_FILE_PATH,
        };

        Cursor c = getContentResolver().query(currentFileUri, projection, null,  null, null);

        c.moveToFirst();
        int filePathColumnIndex = c.getColumnIndex(FilesEntry.COLUMN_RECORDING_FILE_PATH);
        String filePath = c.getString(filePathColumnIndex);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        shareIntent.setType("audio/mp4");
        mContext.startActivity(Intent.createChooser(shareIntent, mContext.getText(R.string.send_to)));
    }

    private void renameFileDialog (final Uri currentFileUri) {
        // File rename dialog
        AlertDialog.Builder renameFileBuilder = new AlertDialog.Builder(FileListActivity.this);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_rename_file, null);

        final EditText input = (EditText) view.findViewById(R.id.new_name);

        renameFileBuilder.setTitle(mContext.getString(R.string.dialog_title_rename));
        renameFileBuilder.setCancelable(true);
        renameFileBuilder.setPositiveButton(mContext.getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            String name = input.getText().toString().trim();

                            String[] projection = {
                                    FilesEntry.COLUMN_RECORDING_NAME,
                            };

                            Cursor c = getContentResolver().query(currentFileUri, projection, null,  null, null);

                            if (!(c.moveToFirst())) {
                                //file name is not unique, cannot rename file.
                                Toast.makeText(mContext,
                                        String.format(mContext.getString(R.string.toast_file_exists), name),
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                String filename = name + ".mp4";
                                //file name is unique, rename file
                                String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                                mFilePath += "/SoundRecorder/" + filename;

                                ContentValues cv = new ContentValues();
                                cv.put(FilesEntry.COLUMN_RECORDING_NAME, name);
                                cv.put(FilesEntry.COLUMN_RECORDING_FILE_PATH, mFilePath);
                                getContentResolver().update(currentFileUri, cv, null, null);
                            }

                        } catch (Exception e) {
                            Log.e(LOG_TAG, "exception", e);
                        }

                        dialog.cancel();
                    }
                });
        renameFileBuilder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        renameFileBuilder.setView(view);
        AlertDialog alert = renameFileBuilder.create();
        alert.show();
    }

    private void addNoteDialog (final Uri currentFileUri) {
        AlertDialog.Builder addNoteBuilder = new AlertDialog.Builder(FileListActivity.this);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_add_note, null);

        final EditText input = (EditText) view.findViewById(R.id.addNote);

        String[] projection = {
                FilesEntry.COLUMN_NOTE,
        };

        Cursor c = getContentResolver().query(currentFileUri, projection, null,  null, null);
        c.moveToFirst();

        int noteColIndex = c.getColumnIndex(FilesEntry.COLUMN_NOTE);

        String note = c.getString(noteColIndex);
        input.setText(note);

        addNoteBuilder.setTitle(mContext.getString(R.string.dialog_title_note));
        addNoteBuilder.setCancelable(true);
        addNoteBuilder.setPositiveButton(mContext.getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        try {
                            String note = input.getText().toString().trim();

                            ContentValues cv = new ContentValues();
                            cv.put(FilesEntry.COLUMN_NOTE, note);
                            getContentResolver().update(currentFileUri, cv, null, null);


                        } catch (Exception e) {
                            Log.e(LOG_TAG, "exception", e);
                        }

                        dialog.cancel();
                    }
                });
        addNoteBuilder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        addNoteBuilder.setView(view);
        AlertDialog alert = addNoteBuilder.create();
        alert.show();
    }

    private void moveFileDialog (final Uri currentFileUri) {
        AlertDialog.Builder moveFileBuilder = new AlertDialog.Builder(FileListActivity.this);

        //LayoutInflater inflater = LayoutInflater.from(mContext);
        //View view = inflater.inflate(R.layout.dialog_folder_list, null);

        moveFileBuilder.setTitle(mContext.getString(R.string.dialog_title_move));
        moveFileBuilder.setCancelable(true);

        String[] projection = new String[] {
                FoldersEntry.COLUMN_FOLDER_NAME
        };
        Cursor c = getContentResolver().query(FoldersEntry.CONTENT_URI, projection, null, null, null);

        ArrayList<String> folderNames = new ArrayList<String>();

        while (c.moveToNext()) {
            int index = c.getColumnIndex(FoldersEntry.COLUMN_FOLDER_NAME);
            folderNames.add(c.getString(index));
        }

        int n = folderNames.size();

        final CharSequence[] items = folderNames.toArray(new CharSequence[folderNames.size()]);

        moveFileBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

                ContentValues cv = new ContentValues();
                cv.put(FilesEntry.COLUMN_FOLDER_ID, i+1);
                getContentResolver().update(currentFileUri, cv, null, null);
            }
        });
        moveFileBuilder.setCancelable(true);
        moveFileBuilder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        //moveFileBuilder.setView(view);
        AlertDialog alert = moveFileBuilder.create();
        alert.show();
    }

    private void deleteFileDialog (final Uri currentFileUri) {

        String[] projection = {
                FilesEntry.COLUMN_RECORDING_NAME,
                FilesEntry.COLUMN_RECORDING_FILE_PATH,
        };

        Cursor c = getContentResolver().query(currentFileUri, projection, null,  null, null);

        c.moveToFirst();
        int fileNameColumnIndex = c.getColumnIndex(FilesEntry.COLUMN_RECORDING_NAME);
        final String fileName = c.getString(fileNameColumnIndex);
        int filePathColumnIndex = c.getColumnIndex(FilesEntry.COLUMN_RECORDING_FILE_PATH);
        final String filePath = c.getString(filePathColumnIndex);

        // File delete confirm
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(FileListActivity.this);
        confirmDelete.setTitle(mContext.getString(R.string.dialog_title_delete));
        confirmDelete.setMessage(mContext.getString(R.string.dialog_text_delete));
        confirmDelete.setCancelable(true);
        confirmDelete.setPositiveButton(mContext.getString(R.string.dialog_action_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            //remove item from database, recyclerview, and storage
                            //remove(position);
                            File file = new File(filePath);
                            file.delete();

                            Toast.makeText(
                                    mContext,
                                    String.format(
                                            mContext.getString(R.string.toast_file_delete),
                                            fileName
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();

                            getContentResolver().delete(currentFileUri, null, null);

                        } catch (Exception e) {
                            Log.e(LOG_TAG, "exception", e);
                        }

                        dialog.cancel();
                    }
                });
        confirmDelete.setNegativeButton(mContext.getString(R.string.dialog_action_no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = confirmDelete.create();
        alert.show();
    }
}
