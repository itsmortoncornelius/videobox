package de.handler.mobile.android.videobox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jmolsmobile.landscapevideocapture.CLog;
import com.jmolsmobile.landscapevideocapture.VideoFile;
import com.jmolsmobile.landscapevideocapture.camera.CameraWrapper;
import com.jmolsmobile.landscapevideocapture.configuration.CaptureConfiguration;
import com.jmolsmobile.landscapevideocapture.recorder.VideoRecorder;
import com.jmolsmobile.landscapevideocapture.recorder.VideoRecorderInterface;
import com.jmolsmobile.landscapevideocapture.view.RecordingButtonInterface;
import com.jmolsmobile.landscapevideocapture.view.VideoCaptureView;

/**
 * A camera fragment.
 */
public class Camera1Fragment extends Fragment implements RecordingButtonInterface, VideoRecorderInterface {
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
    private OnVideoListener mOnVideoListener;
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


    interface OnVideoListener {
        void onVideo(@Nullable String videoPath);
        void onError(@NonNull String message);
    }


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(com.jmolsmobile.landscapevideocapture.R.layout.activity_videocapture, container, false);

		mVideoCaptureView = (VideoCaptureView) view.findViewById(com.jmolsmobile.landscapevideocapture.R.id.videocapture_videocaptureview_vcv);
		if (this.mVideoCaptureView != null) {
			this.initializeRecordingUI();
		}
		this.initializeCaptureConfiguration(savedInstanceState);

		return view;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		getActivity().requestWindowFeature(1);
		getActivity().getWindow().setFlags(1024, 1024);
	}

	@Override
	public void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mToggleMessageReceiver, new IntentFilter(TOGGLE_CAMERA_EVENT));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mSaveVideoMessageReceiver, new IntentFilter(SAVE_VIDEO_EVENT));
	}

	@Override
    public void onPause() {
        if (this.mVideoRecorder != null) {
            this.mVideoRecorder.stopRecording(null);
        }

        this.releaseAllResources();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mToggleMessageReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mSaveVideoMessageReceiver);

        super.onPause();
    }


	@Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        savedInstanceState.putBoolean("com.jmolsmobile.savedrecordedboolean", this.mVideoRecorded);
        savedInstanceState.putString("com.jmolsmobile.savedoutputfilename", this.mVideoFile.getFullPath());
        super.onSaveInstanceState(savedInstanceState);
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
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
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


    public void setOnVideoListener(@NonNull OnVideoListener onVideoListener) {
        mOnVideoListener = onVideoListener;
    }

    public Bitmap getVideoThumbnail() {
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(this.mVideoFile.getFullPath(), 2);
        if (thumbnail == null) {
            CLog.d("VideoCapture_Activity", "Failed to generate video preview");
        }

        return thumbnail;
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
        mOnVideoListener.onVideo(this.mVideoFile.getFullPath());
    }

    private void finishCancelled() {
		mOnVideoListener.onVideo(null);
    }

    private void finishError(String message) {
		mOnVideoListener.onError(message);
    }

    private void releaseAllResources() {
        if (this.mVideoRecorder != null) {
            this.mVideoRecorder.releaseAllResources();
        }
    }

    protected CaptureConfiguration generateCaptureConfiguration() {
        CaptureConfiguration returnConfiguration = getActivity().getIntent().getParcelableExtra("com.jmolsmobile.extracaptureconfiguration");
        if (returnConfiguration == null) {
            returnConfiguration = new CaptureConfiguration();
            CLog.d("VideoCapture_Activity", "No captureconfiguration passed - using default configuration");
        }

        return returnConfiguration;
    }

    private boolean generateVideoRecorded(final Bundle savedInstanceState) {
        return savedInstanceState != null && savedInstanceState.getBoolean("com.jmolsmobile.savedrecordedboolean", false);
    }

    protected VideoFile generateOutputFile(final Bundle savedInstanceState) {
        VideoFile returnFile;
        if (savedInstanceState != null) {
            returnFile = new VideoFile(savedInstanceState.getString("com.jmolsmobile.savedoutputfilename"));
        } else {
            returnFile = new VideoFile(getActivity().getIntent().getStringExtra("com.jmolsmobile.extraoutputfilename"));
        }

        return returnFile;
    }
}
