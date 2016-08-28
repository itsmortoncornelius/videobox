package de.handler.mobile.android.videobox;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.List;

public class CameraDialogFragment extends DialogFragment {
	static final String TITLE = "title" + CameraDialogFragment.class.getName();
	static final String CAMERAS = "message" + CameraDialogFragment.class.getName();

	private OnResultListener onResultListener;

	interface OnResultListener {
		void onResult(@Nullable String cameraId);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String title = args.getString(TITLE, "");
		final List<CameraFragment.CameraSpecs> cameraSpecsList = args.getParcelableArrayList(CAMERAS);
		String[] cameraNames = new String[] {};
		if (cameraSpecsList != null) {
			for (int i = 0; i < cameraSpecsList.size(); i++) {
				CameraFragment.CameraSpecs cameraSpecs = cameraSpecsList.get(i);
				cameraNames[i] = cameraSpecs.name;
			}
		}

		return new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setItems(cameraNames, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (onResultListener != null) {
							onResultListener.onResult(
									cameraSpecsList != null ?
											cameraSpecsList.get(which).cameraId
											: null);
						}
					}
				}).create();
	}

	public void setOnResultListener(OnResultListener onResultListener) {
		this.onResultListener = onResultListener;
	}
}