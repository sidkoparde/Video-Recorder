package com.danielkim.soundrecorder.fragments;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.Toast;

import com.danielkim.soundrecorder.data.DBHelper;
import com.danielkim.soundrecorder.MySharedPreferences;
import com.danielkim.soundrecorder.R;
import com.danielkim.soundrecorder.data.dbContract;
import com.danielkim.soundrecorder.data.dbContract.FilesEntry;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sid on 3/13/18.
 * used tutorials from
 * https://www.youtube.com/playlist?list=PL9jCwTXYWjDIHNEGtsRdCTk79I9-95TbJ
 */

public class VideoRecordFragment extends Fragment {

    private static final String ARG_POSITION = "position";

    private static final String LOG_TAG = VideoRecordFragment.class.getSimpleName();

    private TextureView mTextureView;

    private String mFileName = null;
    private String mFilePath = null;

    private DBHelper mDatabase;

    private MediaRecorder mRecorder = null;

    private CameraDevice mCameraDevice;
    private String mCameraId;
    private static SparseIntArray ORIENTATIONS  = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
    private Size mPreviewSize;

    private Size mVideoSize;

    private int mTotalRotation;

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private FloatingActionButton mRecordImageButton;
    private boolean mIsRecording = false;

    private boolean mStartRecording = true;

    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;

    private Chronometer mChronometer;
    private int mElapsedSeconds = 0;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private static final int REQUEST_VIDEO_CAPTURE = 12345;

    public static VideoRecordFragment newInstance(int position) {
        VideoRecordFragment f = new VideoRecordFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record_video, container, false);

        mTextureView = (TextureView) recordView.findViewById(R.id.textureView);
        mRecordImageButton = (FloatingActionButton) recordView.findViewById(R.id.btnRecord);
        mRecordImageButton.setColorNormal(getResources().getColor(R.color.primary));
        mRecordImageButton.setColorPressed(getResources().getColor(R.color.primary_dark));
        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;

            }
        });

        mChronometer = (Chronometer) recordView.findViewById(R.id.chronometer);
        mDatabase = new DBHelper(getActivity().getApplicationContext());



        return recordView;
    }


    private void onRecord(boolean start){

        if (start) {
            mRecordImageButton.setImageResource(R.drawable.ic_media_stop);
            File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
            if (!folder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                folder.mkdir();
            }

            setFileNameAndPath();

            mChronometer.setVisibility(View.VISIBLE);
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();

            mRecorder = new MediaRecorder();
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(mFilePath);
            mRecorder.setVideoEncodingBitRate(1000000);
            mRecorder.setVideoFrameRate(30);
            mRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioChannels(1);
            if (MySharedPreferences.getPrefHighQuality(getActivity().getApplicationContext())) {
                mRecorder.setAudioSamplingRate(44100);
                mRecorder.setAudioEncodingBitRate(192000);
            }

            mRecorder.setOrientationHint(mTotalRotation);
            try {
                mRecorder.prepare();


                mStartingTimeMillis = System.currentTimeMillis();

                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface previewSurface = new Surface(surfaceTexture);

                Surface recordSurface = mRecorder.getSurface();
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                mCaptureRequestBuilder.addTarget(previewSurface);
                mCaptureRequestBuilder.addTarget(recordSurface);
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                try {
                                    cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                            null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Unable to setup camera preview",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }, null);

                mRecorder.start();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.setVisibility(View.INVISIBLE);

            mRecordImageButton.setImageResource(R.drawable.ic_videocam_white_36dp);

            //getActivity().stopService(intent);
            //allow the screen to turn off again once recording is finished
            try {
                mRecorder.stop();
                startPreview();
                mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
                mRecorder.release();
                //Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_recording_finish) + " " + mFilePath, Toast.LENGTH_LONG).show();
            } catch(Exception e){
                Log.e(LOG_TAG, "exception", e);
            }


            mRecorder = null;

            ContentValues values = new ContentValues();
            values.put(FilesEntry.COLUMN_RECORDING_NAME, mFileName);
            values.put(FilesEntry.COLUMN_ISAUDIO, 0);
            values.put(FilesEntry.COLUMN_RECORDING_FILE_PATH, mFilePath);
            values.put(FilesEntry.COLUMN_RECORDING_LENGTH, mElapsedMillis);
            values.put(FilesEntry.COLUMN_FOLDER_ID, 1);
            values.put(FilesEntry.COLUMN_TIME_ADDED, System.currentTimeMillis());

            getActivity().getContentResolver().insert(FilesEntry.CONTENT_URI, values);

            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }

    }

    private void setFileNameAndPath(){
        int count = 0;
        File f;

        SQLiteDatabase db = mDatabase.getReadableDatabase();

        long numEntries = DatabaseUtils.queryNumEntries(db, FilesEntry.TABLE_NAME);

        do{
            count++;

            mFileName = getString(R.string.default_file_name)
                    + "_" + (numEntries + count) + ".mp4";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/SoundRecorder/" + mFileName;

            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();

        stopBackgroundThread();

        super.onPause();
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId: cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation  = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getActivity().getApplicationContext(),
                                            "Unable to setup camera preview",
                                            Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("SoundRecorder");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }
}
