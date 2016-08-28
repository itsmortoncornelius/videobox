package de.handler.mobile.android.videobox;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static de.handler.mobile.android.videobox.PermissionRequestCode.RequestCodes.REQUEST_CODE_PERMISSION_CAMERA;

public class CameraFragment extends Fragment {
	private static final int REQUEST_CODE_DIALOG_CAMERA = 301;

	private View mFrameLayout;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_camera, container, false);
		mFrameLayout = view.findViewById(R.id.fragment_camera_container);
		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		((AbstractActivity) getActivity()).requestPermission(
				Manifest.permission.CAMERA,
				REQUEST_CODE_PERMISSION_CAMERA);
	}

	public void onPermissionGranted() {
		// TODO open camera
		this.getAvailableCameras();
	}

	private void getAvailableCameras() {
		CameraManager cameraManager =
				(CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
		try {
			final String[] cameraIds = cameraManager.getCameraIdList();
			if (cameraIds.length > 1) {
				this.showCameraChoseDialog(cameraIds, cameraManager, new CameraDialogFragment.OnResultListener() {
					@Override
					public void onResult(@Nullable String cameraId) {
						openCamera(cameraId);
					}
				});
			} else if (cameraIds.length > 0) {
				this.openCamera(cameraIds[0]);
			}
		} catch (CameraAccessException e) {
			((AbstractActivity) getActivity()).showInfo(R.string.error_no_camera_available);
		}
	}

	private void showCameraChoseDialog(@NonNull String[] cameraIds,
									   @NonNull CameraManager cameraManager,
									   @NonNull CameraDialogFragment.OnResultListener onResultListener) throws CameraAccessException {

		ArrayList<CameraSpecs> specsList = new ArrayList<>(cameraIds.length);
		for (String cameraId : cameraIds) {
			CameraCharacteristics cameraCharacteristics =
					cameraManager.getCameraCharacteristics(cameraId);

			Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
			if (lensFacing != null) {
				String name = this.createName(lensFacing);
				specsList.add(new CameraSpecs(cameraId, name, lensFacing));
			}
		}

		CameraDialogFragment dialog = new CameraDialogFragment();
		dialog.setOnResultListener(onResultListener);

		Bundle args = new Bundle();
		args.putString(CameraDialogFragment.TITLE, getString(R.string.camera_chooser_dialog_title));
		args.putParcelableArrayList(CameraDialogFragment.CAMERAS, specsList);

		dialog.setArguments(args);
		dialog.show(getFragmentManager(), "tag");
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

	private void openCamera(@Nullable String cameraId) {
		if (cameraId == null) {
			return;
		}

		((AbstractActivity) getActivity()).showInfo("open camera");
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
