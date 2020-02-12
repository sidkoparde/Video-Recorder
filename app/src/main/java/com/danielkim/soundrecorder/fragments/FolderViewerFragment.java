package com.danielkim.soundrecorder.fragments;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Intent;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.danielkim.soundrecorder.activities.FileListActivity;
import com.danielkim.soundrecorder.adapters.FolderCursorAdapter;
import com.danielkim.soundrecorder.data.DBHelper;
import com.danielkim.soundrecorder.R;
import com.danielkim.soundrecorder.data.dbContract.FoldersEntry;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

/**
 * Created by Daniel on 12/23/2014.
 */
public class FolderViewerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "FolderViewerFragment";

    private static final int DATA_LOADER = 0;

    private DBHelper mDBHelper;
    private int position;
    private FolderCursorAdapter mFolderCursorAdapter;

    private FloatingActionButton mAddButton;

    private Context mContext;

    public static FolderViewerFragment newInstance(int position) {
        FolderViewerFragment f = new FolderViewerFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDBHelper = new DBHelper(getActivity().getApplicationContext());
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
        mContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_folder_viewer, container, false);

        mAddButton = (FloatingActionButton) v.findViewById(R.id.btnAddFolder);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFolderDialog();
            }
        });
        ListView mListView = (ListView) v.findViewById(R.id.listView);

        mFolderCursorAdapter = new FolderCursorAdapter(getActivity().getApplicationContext(), null);
        mListView.setAdapter(mFolderCursorAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), FileListActivity.class);
                Uri currentFolderUri = ContentUris.withAppendedId(FoldersEntry.CONTENT_URI, id);

                intent.setData(currentFolderUri);
                startActivity(intent);
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<String> entrys = new ArrayList<>();
                entrys.add(mContext.getString(R.string.dialog_folder_rename));
                //entrys.add(mContext.getString(R.string.dialog_folder_delete));

                final Uri currentFolderUri = ContentUris.withAppendedId(FoldersEntry.CONTENT_URI, id);

                final CharSequence[] items = entrys.toArray(new CharSequence[entrys.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(mContext.getString(R.string.dialog_title_options));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            renameFolderDialog(currentFolderUri);
                        } /*if (item == 1) {
                            deleteFolderDialog(currentFolderUri);
                        }*/
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

        getLoaderManager().initLoader(DATA_LOADER, null, this);

        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                FoldersEntry._ID,
                FoldersEntry.COLUMN_FOLDER_NAME};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(getActivity().getApplicationContext(),   // Parent activity context
                FoldersEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mFolderCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFolderCursorAdapter.swapCursor(null);
    }

    private void renameFolderDialog (final Uri currentFolderUri) {
        //Folder rename dialog
        AlertDialog.Builder renameFolderBuilder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_rename_folder, null);

        final EditText input = (EditText) view.findViewById(R.id.new_name_folder);

        renameFolderBuilder.setTitle(mContext.getString(R.string.dialog_title_folder_rename));
        renameFolderBuilder.setCancelable(true);
        renameFolderBuilder.setPositiveButton(mContext.getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            String name = input.getText().toString().trim();

                            String[] projection = {
                                    FoldersEntry.COLUMN_FOLDER_NAME,
                            };

                            Cursor c = getActivity().getContentResolver().query(currentFolderUri, projection, null,  null, null);

                            c.moveToFirst();
                            String currentName = c.getString(c.getColumnIndex(FoldersEntry.COLUMN_FOLDER_NAME));

                            if (!(currentName.equals("All"))) {
                                if (!(c.moveToFirst())) {
                                    //file name is not unique, cannot rename file.
                                    Toast.makeText(mContext,
                                            String.format(mContext.getString(R.string.toast_folder_exists), name),
                                            Toast.LENGTH_SHORT).show();

                                } else {
                                    ContentValues cv = new ContentValues();
                                    cv.put(FoldersEntry.COLUMN_FOLDER_NAME, name);
                                    getActivity().getContentResolver().update(currentFolderUri, cv, null, null);
                                }
                            } else if (currentName.equals("All")) {
                                Toast.makeText(mContext, String.format(mContext.getString(R.string.toast_folder_all_rename)),
                                        Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            Log.e(LOG_TAG, "exception", e);
                        }

                        dialog.cancel();
                    }
                });
        renameFolderBuilder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        renameFolderBuilder.setView(view);
        AlertDialog alert = renameFolderBuilder.create();
        alert.show();
    }

    private void addFolderDialog() {
        AlertDialog.Builder addFolderBuilder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_add_folder, null);

        final EditText input = (EditText) view.findViewById(R.id.add_folder_edit_text);

        addFolderBuilder.setTitle(mContext.getString(R.string.dialog_title_folder_add));
        addFolderBuilder.setCancelable(true);

        addFolderBuilder.setPositiveButton(mContext.getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {

                            String folderName = input.getText().toString().trim();

                            if (TextUtils.isEmpty(folderName)) {
                                return;
                            }

                            ContentValues values = new ContentValues();
                            values.put(FoldersEntry.COLUMN_FOLDER_NAME, folderName);

                            getActivity().getContentResolver().insert(FoldersEntry.CONTENT_URI, values);

                        } catch (Exception e) {
                        Log.e(LOG_TAG, "exception", e);
                    }

                        dialog.cancel();
                }
    });
        addFolderBuilder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        addFolderBuilder.setView(view);
        AlertDialog alert = addFolderBuilder.create();
        alert.show();
    }

    private void deleteFolderDialog (final Uri currentFolderUri) {
        String[] projection = {
                FoldersEntry._ID,
                FoldersEntry.COLUMN_FOLDER_NAME
        };

        Cursor c = getActivity().getContentResolver().query(currentFolderUri, projection, null,  null, null);

        c.moveToFirst();
        int folderNameColumnIndex = c.getColumnIndex(FoldersEntry.COLUMN_FOLDER_NAME);
        final String folderName = c.getString(folderNameColumnIndex);
        int folderIDColumnIndex = c.getColumnIndex(FoldersEntry._ID);
        final int folderID = c.getInt(folderIDColumnIndex);
        Log.i(LOG_TAG, String.valueOf(folderID));
        //Log.i(LOG_TAG, folderName);

        /**String[] projectionFile = {
                dbContract.FilesEntry.COLUMN_FOLDER_ID
        };
        Cursor cFile = getActivity().getContentResolver().query(dbContract.FilesEntry.CONTENT_URI, projectionFile, null, null, null);

        while (cFile.moveToNext()) {
            int fileFolderID = cFile.getInt(cFile.getColumnIndex(dbContract.FilesEntry.COLUMN_FOLDER_ID));
                Log.i(LOG_TAG, String.valueOf(fileFolderID));
        }*/

        // Folder delete confirm
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(getActivity());
        confirmDelete.setTitle(mContext.getString(R.string.dialog_title_folder_delete));
        confirmDelete.setMessage(mContext.getString(R.string.dialog_text_delete_folder));
        confirmDelete.setCancelable(true);
        confirmDelete.setPositiveButton(mContext.getString(R.string.dialog_action_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            //remove item from database, recyclerview, and storage
                            //remove(position);
                            //File file = new File(filePath);
                            //file.delete();

                            /**String[] projection = {
                                    dbContract.FilesEntry.COLUMN_FOLDER_ID
                            };
                            Cursor c = getActivity().getContentResolver().query(dbContract.FilesEntry.CONTENT_URI, projection, null, null, null);

                            while (c.moveToNext()) {
                                int fileFolderID = c.getInt(c.getColumnIndex(dbContract.FilesEntry.COLUMN_FOLDER_ID));
                                if (fileFolderID == folderID) {
                                    ContentValues cv = new ContentValues();
                                    cv.put(dbContract.FilesEntry.COLUMN_FOLDER_ID, 1);
                                    getActivity().getContentResolver().update(dbContract.FilesEntry.CONTENT_URI, cv, null, null);
                                    Log.i(LOG_TAG, String.valueOf(fileFolderID));
                                }
                            }*/
                            Toast.makeText(
                                    mContext,
                                    String.format(
                                            mContext.getString(R.string.toast_file_delete),
                                            folderName
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();

                            getActivity().getContentResolver().delete(currentFolderUri, null, null);

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
