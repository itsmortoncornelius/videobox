package de.handler.mobile.android.videobox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.jmolsmobile.landscapevideocapture.CLog;
import com.jmolsmobile.landscapevideocapture.VideoFile;
import com.jmolsmobile.landscapevideocapture.camera.CameraWrapper;
import com.jmolsmobile.landscapevideocapture.configuration.CaptureConfiguration;
import com.jmolsmobile.landscapevideocapture.recorder.VideoRecorder;
import com.jmolsmobile.landscapevideocapture.recorder.VideoRecorderInterface;
import com.jmolsmobile.landscapevideocapture.view.RecordingButtonInterface;
import com.jmolsmobile.landscapevideocapture.view.VideoCaptureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * A camera activity.
 */
public class CameraActivity extends AbstractNearbyActivity implements RecordingButtonInterface, VideoRecorderInterface {
    public static final int RESULT_ERROR = 753245;
    public static final String EXTRA_OUTPUT_FILENAME = "com.jmolsmobile.extraoutputfilename";
    public static final String EXTRA_CAPTURE_CONFIGURATION = "com.jmolsmobile.extracaptureconfiguration";
    public static final String EXTRA_TOGGLE_CAMERA = "com.jmolsmobile.extratogglecamera";
    public static final String EXTRA_SAVE_VIDEO = "com.jmolsmobile.extrasavevideo";

    public static final String TOGGLE_CAMERA_EVENT = "toggle_camera_event";
    public static final String SAVE_VIDEO_EVENT = "save_video_event";

    private boolean mVideoRecorded = false;
    private VideoFile mVideoFile = null;
    private CaptureConfiguration mCaptureConfiguration;
    private VideoCaptureView mVideoCaptureView;
    private VideoRecorder mVideoRecorder;
    private BroadcastReceiver mToggleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Get extra data included in the Intent
            final boolean toggleCamera = intent.getBooleanExtra(EXTRA_TOGGLE_CAMERA, false);
            Log.d("receiver", "Got message: " + toggleCamera);
            if (toggleCamera && null != mVideoRecorder) {
                mVideoRecorder.toggleRecording();
            }
        }
    };
    private BroadcastReceiver mSaveVideoMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            final boolean saveVideo = intent.getBooleanExtra(EXTRA_SAVE_VIDEO, false);
            Log.d("receiver", "Got message: " + saveVideo);
            if (saveVideo) {
                finishCompleted();
            }
        }
    };


    public static File getOutputMediaFile(@NonNull final Context context, final int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        final File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), context.getString(R.string.app_name));
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d(context.getString(R.string.app_name), "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CLog.toggleLogging(this);
        this.requestWindowFeature(1);
        this.getWindow().setFlags(1024, 1024);
        this.setContentView(com.jmolsmobile.landscapevideocapture.R.layout.activity_videocapture);
        this.initializeCaptureConfiguration(savedInstanceState);
        this.mVideoCaptureView = (VideoCaptureView)this.findViewById(com.jmolsmobile.landscapevideocapture.R.id.videocapture_videocaptureview_vcv);
        if (this.mVideoCaptureView != null) {
            this.initializeRecordingUI();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mToggleMessageReceiver, new IntentFilter(TOGGLE_CAMERA_EVENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mSaveVideoMessageReceiver, new IntentFilter(SAVE_VIDEO_EVENT));
    }

    @Override
    protected void onPause() {
        if (this.mVideoRecorder != null) {
            this.mVideoRecorder.stopRecording(null);
        }

        this.releaseAllResources();

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mToggleMessageReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        savedInstanceState.putBoolean("com.jmolsmobile.savedrecordedboolean", this.mVideoRecorded);
        savedInstanceState.putString("com.jmolsmobile.savedoutputfilename", this.mVideoFile.getFullPath());
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onBackPressed() {
        this.finishCancelled();
    }

    @Override
    public void onRecordButtonClicked() {
        this.mVideoRecorder.toggleRecording();
    }

    @Override
    public void onAcceptButtonClicked() {
        this.finishCompleted();
    }

    @Override
    public void onDeclineButtonClicked() {
        this.finishCancelled();
    }

    @Override
    public void onRecordingStarted() {
        this.mVideoCaptureView.updateUIRecordingOngoing();
    }

    @Override
    public void onRecordingStopped(String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        this.mVideoCaptureView.updateUIRecordingFinished(this.getVideoThumbnail());
        this.releaseAllResources();
    }

    @Override
    public void onRecordingSuccess() {
        this.mVideoRecorded = true;
    }

    @Override
    public void onRecordingFailed(String message) {
        this.finishError(message);
    }

    public Bitmap getVideoThumbnail() {
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(this.mVideoFile.getFullPath(), 2);
        if(thumbnail == null) {
            CLog.d("VideoCapture_Activity", "Failed to generate video preview");
        }

        return thumbnail;
    }


    @Override
    protected void onNearbyConnected() {

    }

    @Override
    protected void showCamera() {

    }

    @Override
    protected void showRemote() {

    }

    @Override
    protected void startRecording() {

    }

    @Override
    protected void stopRecording() {

    }

    @Override
    protected void setRootView() {

    }

    @Override
    protected void onPermissionGranted(int requestCodePermission, boolean granted) {

    }

    protected CaptureConfiguration generateCaptureConfiguration() {
        CaptureConfiguration returnConfiguration = this.getIntent().getParcelableExtra("com.jmolsmobile.extracaptureconfiguration");
        if(returnConfiguration == null) {
            returnConfiguration = new CaptureConfiguration();
            CLog.d("VideoCapture_Activity", "No captureconfiguration passed - using default configuration");
        }

        return returnConfiguration;
    }

    protected VideoFile generateOutputFile(final Bundle savedInstanceState) {
        VideoFile returnFile;
        if(savedInstanceState != null) {
            returnFile = new VideoFile(savedInstanceState.getString("com.jmolsmobile.savedoutputfilename"));
        } else {
            returnFile = new VideoFile(this.getIntent().getStringExtra("com.jmolsmobile.extraoutputfilename"));
        }

        return returnFile;
    }


    private void initializeCaptureConfiguration(final Bundle savedInstanceState) {
        this.mCaptureConfiguration = this.generateCaptureConfiguration();
        this.mVideoRecorded = this.generateVideoRecorded(savedInstanceState);
        this.mVideoFile = this.generateOutputFile(savedInstanceState);
    }

    private void initializeRecordingUI() {
        this.mVideoRecorder = new VideoRecorder(this, this.mCaptureConfiguration, this.mVideoFile, new CameraWrapper(), this.mVideoCaptureView.getPreviewSurfaceHolder());
        this.mVideoCaptureView.setRecordingButtonInterface(this);
        if (this.mVideoRecorded) {
            this.mVideoCaptureView.updateUIRecordingFinished(this.getVideoThumbnail());
        } else {
            this.mVideoCaptureView.updateUINotRecording();
        }
    }

    private void finishCompleted() {
        Intent result = new Intent();
        result.putExtra("com.jmolsmobile.extraoutputfilename", this.mVideoFile.getFullPath());
        this.setResult(-1, result);
        this.finish();
    }

    private void finishCancelled() {
        this.setResult(0);
        this.finish();
    }

    private void finishError(String message) {
        Toast.makeText(this.getApplicationContext(), "Can\'t capture video: " + message, Toast.LENGTH_SHORT).show();
        Intent result = new Intent();
        result.putExtra("com.jmolsmobile.extraerrormessage", message);
        this.setResult(RESULT_ERROR, result);
        this.finish();
    }

    private void releaseAllResources() {
        if(this.mVideoRecorder != null) {
            this.mVideoRecorder.releaseAllResources();
        }
    }

    private boolean generateVideoRecorded(final Bundle savedInstanceState) {
        return savedInstanceState != null && savedInstanceState.getBoolean("com.jmolsmobile.savedrecordedboolean", false);
    }
}
