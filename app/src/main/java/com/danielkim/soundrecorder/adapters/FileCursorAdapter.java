package com.danielkim.soundrecorder.adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.danielkim.soundrecorder.R;
import com.danielkim.soundrecorder.data.dbContract.FilesEntry;

import java.util.concurrent.TimeUnit;

/**
 * Created by sid on 3/27/18.
 */

public class FileCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = "FileCursorAdapter";

    private Context mContext;

    public FileCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);

        mContext = context;

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.card_view, viewGroup, false);

    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView vName = (TextView) view.findViewById(R.id.file_name_text);
        TextView vLength = (TextView) view.findViewById(R.id.file_length_text);
        TextView vDateAdded = (TextView) view.findViewById(R.id.file_date_added_text);

        int fileNameColumnIndex = cursor.getColumnIndex(FilesEntry.COLUMN_RECORDING_NAME);
        int fileLengthColumnIndex = cursor.getColumnIndex(FilesEntry.COLUMN_RECORDING_LENGTH);
        int fileIsAudioColumnIndex = cursor.getColumnIndex(FilesEntry.COLUMN_ISAUDIO);
        int fileTimeAddedColumnIndex = cursor.getColumnIndex(FilesEntry.COLUMN_TIME_ADDED);

        String name = cursor.getString(fileNameColumnIndex);
        int fileLength = cursor.getInt(fileLengthColumnIndex);
        int fileIsAudio = cursor.getInt(fileIsAudioColumnIndex);
        int fileTimeAdded = cursor.getInt(fileTimeAddedColumnIndex);

        if (fileIsAudio == 0) {
            ImageView vImage = (ImageView) view.findViewById(R.id.imageView);
            vImage.setImageResource(R.drawable.ic_videocam_white_36dp);
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(fileLength);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(fileLength)
                - TimeUnit.MINUTES.toSeconds(minutes);

        vLength.setText(String.format("%02d:%02d", minutes, seconds));

        vDateAdded.setText(
                DateUtils.formatDateTime(
                        mContext,
                        fileTimeAdded,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR
                )
        );

        vName.setText(name);

    }
}
