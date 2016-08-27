package de.handler.mobile.android.videobox;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public abstract class AbstractActivity extends AppCompatActivity {
	protected View mRootView;

	protected abstract void setRootView();

	@Override
	protected void onResume() {
		super.onResume();
		this.setRootView();
	}

	@SafeVarargs
	protected final void startActivity(@NonNull Context context, @NonNull Class<?> target, Pair<String, String>... args) {
		Intent intent = new Intent(context, target);
		for (Pair<String, String> pair : args) {
			intent.putExtra(pair.first, pair.second);
		}
		super.startActivity(intent);
	}

	protected Toolbar setToolbar(@IdRes int toolbarRes) {
		Toolbar toolbar = (Toolbar) findViewById(toolbarRes);
		setSupportActionBar(toolbar);
		return toolbar;
	}

	protected void showInfo(@StringRes int message) {
		this.showInfo(message, mRootView);
	}

	protected void showInfo(@NonNull String message) {
		this.showInfo(message, mRootView);
	}

	protected void showInfo(@StringRes int message, @NonNull View view) {
		this.showInfo(message, view, Snackbar.LENGTH_LONG);
	}

	protected void showInfo(@NonNull String message, @NonNull View view) {
		this.showInfo(message, view, Snackbar.LENGTH_LONG);
	}

	protected void showInfo(@StringRes int message, @NonNull View view, @DisplayLength int length) {
		Snackbar.make(view, message, length).show();
	}

	protected void showInfo(@NonNull String message, @NonNull View view, @DisplayLength int length) {
		Snackbar.make(view, message, length).show();
	}

}
