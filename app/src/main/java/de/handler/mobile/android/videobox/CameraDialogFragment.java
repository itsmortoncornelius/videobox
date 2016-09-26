package de.handler.mobile.android.videobox;

import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class CameraDialogFragment extends DialogFragment {
	static final String TITLE = "title" + CameraDialogFragment.class.getName();
	static final String CAMERAS = "message" + CameraDialogFragment.class.getName();

	private OnResultListener onResultListener;

	interface OnResultListener {
		void onResult(@Nullable CameraFragment.CameraSpecs cameraSpecs) throws CameraAccessException, InterruptedException;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String title = args.getString(TITLE, "");
		final List<CameraFragment.CameraSpecs> cameraSpecsList = args.getParcelableArrayList(CAMERAS);
		List<String> cameraNames = new ArrayList<>();
		if (cameraSpecsList != null) {
			for (int i = 0; i < cameraSpecsList.size(); i++) {
				CameraFragment.CameraSpecs cameraSpecs = cameraSpecsList.get(i);
				cameraNames.add(cameraSpecs.name);
			}
		}
		String[] cameraNameArray = new String[cameraNames.size()];
		cameraNames.toArray(cameraNameArray);

		return new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setItems(cameraNameArray, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (onResultListener != null) {
							try {
								onResultListener.onResult(
										cameraSpecsList != null ?
												cameraSpecsList.get(which)
												: null);
							} catch (CameraAccessException | InterruptedException e) {
								((AbstractActivity) getActivity()).showInfo(R.string.error_camera_not_accessible);
							}
						}
					}
				}).create();
	}

	public void setOnResultListener(OnResultListener onResultListener) {
		this.onResultListener = onResultListener;
	}
}
