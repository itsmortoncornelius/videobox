package de.handler.mobile.android.videobox;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class CameraFragment extends Fragment {
	private static final double CAMERA_RATIO = 4 / 3;
	private static final int MAX_P = 1080;

	private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
	private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
	private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
	private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

	static {
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
	}

	static {
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
	}

	private Size mVideoSize;
	private Size mPreviewSize;
	private CameraDevice mCamera;
	private CameraView mCameraView;
	private Integer mSensorOrientation;
	private Handler mBackgroundHandler;
	private MediaRecorder mMediaRecorder;
	@Nullable
	private String mNextVideoAbsolutePath;
	private HandlerThread mBackgroundThread;
	private CameraCaptureSession mPreviewSession;
	private CaptureRequest.Builder mPreviewBuilder;
	// prevent the app from closing before the camera stops
	private Semaphore mCameraOpenCloseLock = new Semaphore(1);
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private boolean mVideoRecording;
	private boolean mHasMoreCameras = false;

	/**
	 * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
	 * {@link TextureView}.
	 */
	private TextureView.SurfaceTextureListener mSurfaceTextureListener
			= new TextureView.SurfaceTextureListener() {
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
											  int width, int height) {
			mSurfaceWidth = width;
			mSurfaceHeight = height;

			getAvailableCameras(CameraDevice.TEMPLATE_PREVIEW);
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
												int width, int height) {
			configureTransform(width, height);
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}
	};


	private static File getOutputMediaFile(@NonNull final Context context, final int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		final File mediaStorageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), context.getString(R.string.app_name));
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(context.getString(R.string.app_name), "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}


	private static Size chooseVideoSize(Size[] choices) {
		for (Size size : choices) {
			if (size.getWidth() == size.getHeight() * CAMERA_RATIO && size.getWidth() <= MAX_P) {
				return size;
			}
		}
		return choices[choices.length - 1];
	}

	private static Size chooseOptimalSize(@NonNull Size[] choices, int width, int height, @NonNull Size aspectRatio) {
		// Collect the supported resolutions that are at least as big as the preview Surface
		List<Size> bigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for (Size option : choices) {
			if (option.getHeight() == option.getWidth() * h / w &&
					option.getWidth() >= width && option.getHeight() >= height) {
				bigEnough.add(option);
			}
		}

		// Pick the smallest of those, assuming we found any
		if (bigEnough.size() > 0) {
			return Collections.min(bigEnough, new Camera2VideoFragment.CompareSizesByArea());
		} else {
			return choices[0];
		}
	}



	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_camera, container, false);
		mCameraView = (CameraView) view.findViewById(R.id.fragment_camera_view);
		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			this.setUpMediaRecorder();
		} catch (IOException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_media_recorder_not_accessible);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startBackgroundThread();
		if (mCameraView.isAvailable()) {
			this.getAvailableCameras(CameraDevice.TEMPLATE_PREVIEW);
		} else {
			mCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
		}
	}

	@Override
	public void onPause() {
		closeCamera();
		stopBackgroundThread();
		super.onPause();
	}


	public void startRecordingVideo() {
		this.getAvailableCameras(CameraDevice.TEMPLATE_RECORD);
	}

	public void stopRecordingVideo() {
		mVideoRecording = false;
		mMediaRecorder.stop();
		mMediaRecorder.reset();

		mNextVideoAbsolutePath = null;
		startPreview(mCamera, CameraDevice.TEMPLATE_PREVIEW);
	}


	private void startBackgroundThread() {
		mBackgroundThread = new HandlerThread("CameraBackground");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	private void stopBackgroundThread() {
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void closeCamera() {
		try {
			mCameraOpenCloseLock.acquire();
			closePreviewSession();
			if (null != mCamera) {
				mCamera.close();
				mCamera = null;
			}
			if (null != mMediaRecorder) {
				mMediaRecorder.release();
				mMediaRecorder = null;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while trying to lock camera closing.");
		} finally {
			mCameraOpenCloseLock.release();
		}
	}

	private void setUpMediaRecorder() throws IOException {
		final Activity activity = getActivity();
		if (null == activity) {
			return;
		}
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		if (TextUtils.isEmpty(mNextVideoAbsolutePath)) {
			File outputMediaFile = getOutputMediaFile(getActivity(), MEDIA_TYPE_VIDEO);
			mNextVideoAbsolutePath = outputMediaFile != null ? outputMediaFile.getAbsolutePath() : null;
		}
		mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
		mMediaRecorder.setVideoEncodingBitRate(10000000);
		mMediaRecorder.setVideoFrameRate(30);
		mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		switch (mSensorOrientation) {
			case SENSOR_ORIENTATION_DEFAULT_DEGREES:
				mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
				break;
			case SENSOR_ORIENTATION_INVERSE_DEGREES:
				mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
				break;
		}
		mMediaRecorder.prepare();
	}

	private void getAvailableCameras(final int cameraTemplate) {
		final CameraManager cameraManager =
				(CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
		try {
			final String[] cameraIds = cameraManager.getCameraIdList();
			if (cameraIds.length > 1) {
				mHasMoreCameras = true;
				this.showCameraChoseDialog(this.getCameraSpecs(cameraIds, cameraManager), new CameraDialogFragment.OnResultListener() {
					@Override
					public void onResult(@Nullable CameraSpecs cameraSpecs) throws CameraAccessException, InterruptedException {
						openCamera(cameraSpecs, cameraManager, cameraTemplate);
					}
				});
			} else if (cameraIds.length > 0) {
				mHasMoreCameras = false;
				CameraSpecs cameraSpecs = this.getCameraSpecs(cameraIds, cameraManager).get(0);
				this.openCamera(cameraSpecs, cameraManager, cameraTemplate);
			}
		} catch (CameraAccessException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
			if (mHasMoreCameras) {
				this.getAvailableCameras(cameraTemplate);
			}
		} catch (InterruptedException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
		}
	}

	private List<CameraSpecs> getCameraSpecs(String[] cameraIds, CameraManager cameraManager) throws CameraAccessException {
		List<CameraSpecs> specsList = new ArrayList<>(cameraIds.length);
		for (String cameraId : cameraIds) {
			CameraCharacteristics cameraCharacteristics =
					cameraManager.getCameraCharacteristics(cameraId);

			Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
			if (lensFacing != null) {
				String name = this.createName(lensFacing);
				specsList.add(new CameraSpecs(cameraId, name, lensFacing));
			}
		}
		return specsList;
	}

	private void showCameraChoseDialog(@NonNull List<CameraSpecs> specsList,
									   @NonNull CameraDialogFragment.OnResultListener onResultListener) throws CameraAccessException {

		CameraDialogFragment dialog = new CameraDialogFragment();
		dialog.setOnResultListener(onResultListener);

		Bundle args = new Bundle();
		args.putString(CameraDialogFragment.TITLE, getString(R.string.camera_chooser_dialog_title));
		args.putParcelableArrayList(CameraDialogFragment.CAMERAS, new ArrayList<Parcelable>(specsList));

		dialog.setArguments(args);
		dialog.show(getFragmentManager(), CameraDialogFragment.class.getCanonicalName());
	}

	private String createName(int lensFacing) {
		switch (lensFacing) {
			case LENS_FACING_BACK:
				return getString(R.string.camera_back);
			case LENS_FACING_FRONT:
				return getString(R.string.camera_front);
			case LENS_FACING_EXTERNAL:
				return getString(R.string.camera_external);
		}
		return null;
	}

	private void openCamera(@Nullable CameraSpecs cameraSpecs, @NonNull final CameraManager cameraManager, final int cameraTemplate)
			throws CameraAccessException, InterruptedException {
		if (cameraSpecs == null) {
			return;
		}

		((AbstractActivity) getActivity()).showInfo("open camera");
		if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			return;
		}

		if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
			throw new RuntimeException("Time out waiting to lock camera opening.");
		}

		// Choose the sizes for camera preview and video recording
		CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraSpecs.cameraId);
		StreamConfigurationMap map = characteristics
				.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		if (map == null) {
			return;
		}
		mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
		mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
		mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
				mSurfaceWidth, mSurfaceHeight, mVideoSize);

		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mCameraView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
		} else {
			mCameraView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
		}
		configureTransform(mSurfaceWidth, mSurfaceHeight);
		mMediaRecorder = new MediaRecorder();

		cameraManager.openCamera(cameraSpecs.cameraId, new CameraDevice.StateCallback() {
			@Override
			public void onOpened(@NonNull CameraDevice camera) {
				try {
					mCamera = camera;
					createCameraSession(camera, cameraTemplate);
				} catch (CameraAccessException e) {
					((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
				}
			}

			@Override
			public void onDisconnected(@NonNull CameraDevice camera) {
				// Another application has requested the camera
				((AbstractActivity) getActivity()).showInfo(R.string.error_camera_disconnected);
				mCameraOpenCloseLock.release();
				camera.close();
			}

			@Override
			public void onError(@NonNull CameraDevice camera, int error) {
				mCameraOpenCloseLock.release();
				camera.close();
				AbstractActivity activity = ((AbstractActivity) getActivity());
				switch (error) {
					case ERROR_CAMERA_IN_USE:
						activity.showInfo(R.string.error_camera_in_use);
						break;
					case ERROR_MAX_CAMERAS_IN_USE:
						activity.showInfo(R.string.error_camera_max_in_use);
						break;
					case ERROR_CAMERA_DISABLED:
						activity.showInfo(R.string.error_camera_disabled);
						break;
					case ERROR_CAMERA_DEVICE:
						activity.showInfo(R.string.error_camera_fatal);
						break;
					case ERROR_CAMERA_SERVICE:
						activity.showInfo(R.string.error_camera_service);
						break;
				}
			}
		}, new Handler());
	}

	private void createCameraSession(@NonNull CameraDevice camera, int cameraTemplate) throws CameraAccessException {
		this.startPreview(camera, cameraTemplate);
		mCameraOpenCloseLock.release();
		if (mCameraView != null) {
			this.configureTransform(mCameraView.getWidth(), mCameraView.getHeight());
		}
	}

	private void startPreview(final CameraDevice camera, int type) {
		if (null == camera || !mCameraView.isAvailable() || null == mPreviewSize) {
			return;
		}
		try {
			this.closePreviewSession();
			SurfaceTexture texture = mCameraView.getSurfaceTexture();
			assert texture != null;
			texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

			mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
			List<Surface> surfaces = new ArrayList<>();

			// Set up Surface for the camera preview
			Surface previewSurface = new Surface(texture);
			surfaces.add(previewSurface);
			mPreviewBuilder.addTarget(previewSurface);


			mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

			camera.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
				@Override
				public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					mPreviewSession = cameraCaptureSession;
					updatePreview(camera);
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
					((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
				}
			}, mBackgroundHandler);
		} catch (CameraAccessException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
		}
	}

	private void closePreviewSession() {
		if (mPreviewSession != null) {
			mPreviewSession.close();
			mPreviewSession = null;
		}
	}

	private void updatePreview(CameraDevice camera) {
		if (null == camera) {
			return;
		}
		try {
			setUpCaptureRequestBuilder(mPreviewBuilder);
			HandlerThread thread = new HandlerThread("CameraPreview");
			thread.start();
			mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
		} catch (CameraAccessException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
		}
	}

	private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
		builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
	}

	/**
	 * Configures the necessary {@link android.graphics.Matrix} transformation to `mCameraView`.
	 * This method should not to be called until the camera preview size is determined in
	 * openCamera, or until the size of `mCameraView` is fixed.
	 *
	 * @param viewWidth  The width of `mCameraView`
	 * @param viewHeight The height of `mCameraView`
	 */
	private void configureTransform(int viewWidth, int viewHeight) {
		Activity activity = getActivity();
		if (null == mCameraView || null == mPreviewSize || null == activity) {
			return;
		}
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		Matrix matrix = new Matrix();
		RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
		RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
		float centerX = viewRect.centerX();
		float centerY = viewRect.centerY();
		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
			float scale = Math.max(
					(float) viewHeight / mPreviewSize.getHeight(),
					(float) viewWidth / mPreviewSize.getWidth());
			matrix.postScale(scale, scale, centerX, centerY);
			matrix.postRotate(90 * (rotation - 2), centerX, centerY);
		}
		mCameraView.setTransform(matrix);
	}


	class CameraSpecs implements Parcelable {
		final String cameraId;
		final String name;
		final int facing;

		private CameraSpecs(@NonNull String cameraId, String name, int facing) {
			this.cameraId = cameraId;
			this.facing = facing;
			this.name = name;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(this.cameraId);
			dest.writeString(this.name);
			dest.writeInt(this.facing);
		}

		CameraSpecs(Parcel in) {
			this.cameraId = in.readString();
			this.name = in.readString();
			this.facing = in.readInt();
		}

		public final Parcelable.Creator<CameraSpecs> CREATOR = new Parcelable.Creator<CameraSpecs>() {
			@Override
			public CameraSpecs createFromParcel(Parcel source) {
				return new CameraSpecs(source);
			}

			@Override
			public CameraSpecs[] newArray(int size) {
				return new CameraSpecs[size];
			}
		};
	}
}
