package com.danielkim.soundrecorder.adapters;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.danielkim.soundrecorder.data.DBHelper;
import com.danielkim.soundrecorder.activities.FileListActivity;
import com.danielkim.soundrecorder.FolderItem;
import com.danielkim.soundrecorder.R;

import com.danielkim.soundrecorder.data.dbContract.FoldersEntry;
import com.danielkim.soundrecorder.listeners.OnDatabaseChangedListener;

/**
 * Created by Daniel on 12/29/2014.
 */
public class FolderCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = "FolderCursorAdapter";

    public FolderCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {

        return LayoutInflater.from(context).inflate(R.layout.card_view_folder, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameTextView = (TextView) view.findViewById(R.id.folder_name_text);

        int nameColumnIndex = cursor.getColumnIndex(FoldersEntry.COLUMN_FOLDER_NAME);

        String folderName = cursor.getString(nameColumnIndex);

        nameTextView.setText(folderName);
    }

}
