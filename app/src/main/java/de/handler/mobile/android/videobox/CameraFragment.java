package de.handler.mobile.android.videobox;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class CameraFragment extends Fragment {
	private CameraView mCameraView;
	private boolean mHasMoreCameras = false;


	public static File getOutputMediaFile(@NonNull final Context context, final int type) {
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
		this.getAvailableCameras();
	}

	private void getAvailableCameras() {
		final CameraManager cameraManager =
				(CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
		try {
			final String[] cameraIds = cameraManager.getCameraIdList();
			if (cameraIds.length > 1) {
				mHasMoreCameras = true;
				this.showCameraChoseDialog(this.getCameraSpecs(cameraIds, cameraManager), new CameraDialogFragment.OnResultListener() {
					@Override
					public void onResult(@Nullable CameraSpecs cameraSpecs) throws CameraAccessException {
						openCamera(cameraSpecs, cameraManager);
					}
				});
			} else if (cameraIds.length > 0) {
				mHasMoreCameras = false;
				CameraSpecs cameraSpecs = this.getCameraSpecs(cameraIds, cameraManager).get(0);
				this.openCamera(cameraSpecs, cameraManager);
			}
		} catch (CameraAccessException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
			if (mHasMoreCameras) {
				this.getAvailableCameras();
			}
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

	private void openCamera(@Nullable CameraSpecs cameraSpecs, @NonNull final CameraManager cameraManager) throws CameraAccessException {
		if (cameraSpecs == null) {
			return;
		}

		((AbstractActivity) getActivity()).showInfo("open camera");
		if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			return;
		}

		cameraManager.openCamera(cameraSpecs.cameraId, new CameraDevice.StateCallback() {
			@Override
			public void onOpened(@NonNull CameraDevice camera) {
				try {
					createCameraSession(camera);
				} catch (CameraAccessException e) {
					((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
				}
			}

			@Override
			public void onDisconnected(@NonNull CameraDevice camera) {
				// Another application has requested the camera
				((AbstractActivity) getActivity()).showInfo(R.string.error_camera_disconnected);
			}

			@Override
			public void onError(@NonNull CameraDevice camera, int error) {
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

	private void createCameraSession(@NonNull CameraDevice camera) throws CameraAccessException {
		List<Surface> surfaceViews = new ArrayList<>(1);

		// TODO: continue here and before that in CameraView
		camera.createCaptureSession(surfaceViews, new CameraCaptureSession.StateCallback() {
			@Override
			public void onConfigured(@NonNull CameraCaptureSession session) {

			}

			@Override
			public void onConfigureFailed(@NonNull CameraCaptureSession session) {

			}
		}, new Handler());
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
