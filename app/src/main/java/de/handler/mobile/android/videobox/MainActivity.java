package de.handler.mobile.android.videobox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.github.florent37.camerafragment.CameraFragment;
import com.github.florent37.camerafragment.configuration.Configuration;
import com.github.florent37.camerafragment.listeners.CameraFragmentResultListener;

import static de.handler.mobile.android.videobox.PermissionRequestCode.RequestCodes.REQUEST_CODE_PERMISSION_CAMERA;

public class MainActivity extends AbstractNearbyActivity {
	private static final int FRAGMENT_CONTAINER = R.id.main_container;
	private static final String TAG_CAMERA_FRAGMENT = "camera_fragment" + MainActivity.class.getCanonicalName();
	private static final String TAG_WELCOME_FRAGMENT = "welcome_fragment" + MainActivity.class.getCanonicalName();
	private static final String FILE_NAME = "videobox";
	private int counter = 0;
	private ImageButton toggleButton;
	private ProgressBar progressBar;
	private Drawable drawablePlay;
	private Drawable drawableStop;


	@Override
	protected void setRootView() {
		mRootView = findViewById(FRAGMENT_CONTAINER);
		mRootViewId = FRAGMENT_CONTAINER;
	}

	@Override
	protected void onPermissionGranted(int requestCodePermission, boolean granted) {
		if (requestCodePermission == REQUEST_CODE_PERMISSION_CAMERA) {
			if (granted) {
				@SuppressLint("MissingPermission")
				CameraFragment cameraFragment = CameraFragment.newInstance(
						new Configuration.Builder()
								.setFlashMode(Configuration.FLASH_MODE_OFF)
								.setMediaAction(Configuration.MEDIA_ACTION_VIDEO)
								.build());
				replaceFragment(getSupportFragmentManager(), cameraFragment, FRAGMENT_CONTAINER, TAG_CAMERA_FRAGMENT, true);
			} else {
				showInfo(R.string.error_permission_camera);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = setToolbar(R.id.toolbar);
		setSupportActionBar(toolbar);

		drawablePlay = getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp, getTheme());
		drawableStop = getResources().getDrawable(R.drawable.ic_stop_white_24dp, getTheme());

		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		toggleButton = (ImageButton) findViewById(R.id.toggle_button);
		toggleButton.setOnClickListener(v -> {
			progressBar.setVisibility(View.VISIBLE);
			publish(MessageHelper.CONNECTED);
		});

		replaceFragment(getSupportFragmentManager(), new WelcomeFragment(), FRAGMENT_CONTAINER, TAG_WELCOME_FRAGMENT, false);
	}

	@Override
	public void onBackPressed() {
		unpublish();
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CAMERA_FRAGMENT);
		if (fragment instanceof CameraFragment) {
			toggleButton.setVisibility(View.VISIBLE);
		}
		super.onBackPressed();
	}

	@Override
	protected void onNearbyConnected() {
		subscribe();
	}

	@Override
	protected void showRemote() {
		toggleButton.setOnClickListener(v -> {
			publish(MessageHelper.TOGGLE_CAMERA);
			progressBar.setVisibility(View.VISIBLE);
		});
		progressBar.setVisibility(View.GONE);
	}

	@Override
	protected void showCamera() {
		findViewById(R.id.toggle_button).setVisibility(View.GONE);
		requestPermissionz(
				new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
				REQUEST_CODE_PERMISSION_CAMERA);
	}

	@Override
	protected void toggleCamera() {
		progressBar.setVisibility(View.GONE);
		toggleButton.setImageDrawable(drawableStop);
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_CAMERA_FRAGMENT);
		if (fragment instanceof CameraFragment) {
			((CameraFragment) fragment).takePhotoOrCaptureVideo(
					new CameraFragmentResultListener() {
						@Override
						public void onVideoRecorded(String filePath) {
							showInfo(R.string.message_video_successfully_recorded);
							counter += counter;
							toggleButton.setImageDrawable(drawablePlay);
						}

						@Override
						public void onPhotoTaken(byte[] bytes, String filePath) {
							// not implemented here
						}
					}, Environment.DIRECTORY_MOVIES, FILE_NAME + "_" + counter);
		}
	}
}
