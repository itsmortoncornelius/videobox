package de.handler.mobile.android.videobox;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.ViewGroup;


class CameraView extends ViewGroup implements SurfaceHolder.Callback {
	public CameraView(Context context) {
		super(context);
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	}
}
