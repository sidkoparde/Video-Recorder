package com.danielkim.soundrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.danielkim.soundrecorder.activities.MainActivity;
import com.danielkim.soundrecorder.data.DBHelper;
import com.danielkim.soundrecorder.data.dbContract.FilesEntry;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Daniel on 12/28/2014.
 */
public class RecordingService extends Service {

    private static final String LOG_TAG = "RecordingService";

    private String mFileName = null;
    private String mFilePath = null;

    private char mRecordingType;

    private MediaRecorder mRecorder = null;

    private int mVideoHeight;
    private int mVideoWidth;
    private int mTotalRotation;

    private CameraDevice mCameraDevice;

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private DBHelper mDatabase;

    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;
    private int mElapsedSeconds = 0;
    private OnTimerChangedListener onTimerChangedListener = null;
    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private Timer mTimer = null;
    private TimerTask mIncrementTimerTask = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public interface OnTimerChangedListener {
        void onTimerChanged(int seconds);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabase = new DBHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = intent.getExtras();

        mRecordingType = intent.getCharExtra("type", 'a');
        mVideoHeight = intent.getIntExtra("videoHeight", 0);
        mVideoWidth = intent.getIntExtra("videoWidth", 0);
        mTotalRotation = intent.getIntExtra("totalRotation", 0);

        if (mRecordingType == 'a') {
            startRecording();
        } else {
            startVideoRecording();
        }

        return START_STICKY;
    }

    public void startVideoRecording() {
        Log.i(LOG_TAG, "startVideoRecording");
        setFileNameAndPath();

        mRecorder = new MediaRecorder();
        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setVideoEncodingBitRate(1000000);
        mRecorder.setVideoFrameRate(30);
        mRecorder.setVideoSize(mVideoWidth, mVideoHeight);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setOrientationHint(mTotalRotation);
        try {
            mRecorder.prepare();
            mRecorder.start();

            mStartingTimeMillis = System.currentTimeMillis();

            Surface recordSurface = mRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(recordSurface);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
        if (mRecorder != null) {

            stopRecording();
        }

        super.onDestroy();
    }

    public void startRecording() {
        setFileNameAndPath();


        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);
        if (MySharedPreferences.getPrefHighQuality(this)) {
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setAudioEncodingBitRate(192000);
        }

        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();

            startTimer();
            startForeground(1, createNotification());

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void setFileNameAndPath(){
        int count = 0;
        File f;

        do{
            count++;

            mFileName = getString(R.string.default_file_name)
                    + "_" + (mDatabase.getCount() + count) + ".mp4";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/SoundRecorder/" + mFileName;

            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }

    public void stopRecording() {
        Log.i(LOG_TAG, "stopRecording");
        try {
            mRecorder.stop();
            mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
            mRecorder.release();
            //Toast.makeText(this,"Recording saved", Toast.LENGTH_LONG).show();
        } catch(Exception e){
            Log.e(LOG_TAG, "exception", e);
        }

        //remove notification
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.cancel(1);

        if (mIncrementTimerTask != null) {
            mIncrementTimerTask.cancel();
            mIncrementTimerTask = null;
        }

        mRecorder = null;

        ContentValues values = new ContentValues();
        values.put(FilesEntry.COLUMN_RECORDING_NAME, mFileName);
        values.put(FilesEntry.COLUMN_ISAUDIO, 1);
        values.put(FilesEntry.COLUMN_RECORDING_FILE_PATH, mFilePath);
        values.put(FilesEntry.COLUMN_RECORDING_LENGTH, mElapsedMillis);
        values.put(FilesEntry.COLUMN_FOLDER_ID, 1);
        values.put(FilesEntry.COLUMN_TIME_ADDED, System.currentTimeMillis());

        getContentResolver().insert(FilesEntry.CONTENT_URI, values);
    }


    private void startTimer() {
        mTimer = new Timer();
        mIncrementTimerTask = new TimerTask() {
            @Override
            public void run() {
                mElapsedSeconds++;
                if (onTimerChangedListener != null)
                    onTimerChangedListener.onTimerChanged(mElapsedSeconds);
                NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mgr.notify(1, createNotification());
            }
        };
        mTimer.scheduleAtFixedRate(mIncrementTimerTask, 1000, 1000);
    }

    //TODO:
    private Notification createNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_mic_white_36dp)
                        .setContentTitle(getString(R.string.notification_recording))
                        .setContentText(mTimerFormat.format(mElapsedSeconds * 1000))
                        .setOngoing(true);

        mBuilder.setContentIntent(PendingIntent.getActivities(getApplicationContext(), 0,
                new Intent[]{new Intent(getApplicationContext(), MainActivity.class)}, 0));

        return mBuilder.build();
    }
}
