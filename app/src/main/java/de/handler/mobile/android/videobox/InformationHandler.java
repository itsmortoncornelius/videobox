package de.handler.mobile.android.videobox;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

final class InformationHandler {
	static void showInfo(@StringRes int message, @NonNull View view) {
		Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
	}
}
