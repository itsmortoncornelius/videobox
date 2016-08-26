package de.handler.mobile.android.videobox;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public abstract class AbstractActivity extends AppCompatActivity {
	protected Toolbar setToolbar(@IdRes int toolbarRes) {
		Toolbar toolbar = (Toolbar) findViewById(toolbarRes);
		setSupportActionBar(toolbar);
		return toolbar;
	}

	@SafeVarargs
	protected final void startActivity(@NonNull Context context, @NonNull Class<?> target, Pair<String, String>... args) {
		Intent intent = new Intent(context, target);
		for (Pair<String, String> pair : args) {
			intent.putExtra(pair.first, pair.second);
		}
		super.startActivity(intent);
	}
}
