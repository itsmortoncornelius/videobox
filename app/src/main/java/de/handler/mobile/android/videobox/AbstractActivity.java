package de.handler.mobile.android.videobox;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractActivity extends AppCompatActivity {
	protected View mRootView;
	@IdRes
	protected int mRootViewId;

	protected abstract void setRootView();

	protected abstract void onPermissionGranted(int requestCodePermission, boolean granted);

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
		this.showInfo(getString(message), view, length);
	}

	protected void showInfo(@NonNull String message, @NonNull View view, @DisplayLength int length) {
		Snackbar.make(view, message, length).show();
	}

	protected void replaceFragment(@NonNull final FragmentManager fragmentManager,
								   @NonNull final Fragment fragment,
								   @IdRes final int container,
								   @Nullable final String tag) {
		new Handler().post(() ->
				fragmentManager.beginTransaction()
						.replace(container, fragment, tag)
						.commit());
	}

	protected void requestPermission(@NonNull @Permission String permission,
									 @PermissionRequestCode int permissionRequestCode) {
		if (ContextCompat.checkSelfPermission(this, permission)
				!= PackageManager.PERMISSION_GRANTED) {

			// No explanation needed, we can request the permission.
			ActivityCompat.requestPermissions(this,
					new String[]{permission},
					permissionRequestCode);
		} else {
			this.onPermissionGranted(permissionRequestCode, true);
		}
	}

	protected void requestPermissionz(@NonNull @Permission String[] permissions,
									  @PermissionRequestCode int permissionRequestCode) {
		String[] ungrantedPermissions = new String[permissions.length];
		int counter = 0;
		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(this, permission)
					!= PackageManager.PERMISSION_GRANTED) {
				ungrantedPermissions[counter] = permission;
				counter++;
			}
		}
		// No explanation needed, we can request the permission.
		ActivityCompat.requestPermissions(this,
				permissions,
				permissionRequestCode);

		if (ungrantedPermissions.length == 0) {
			this.onPermissionGranted(permissionRequestCode, true);
		}
	}


	@Override
	public void onRequestPermissionsResult(int permissionRequestCode,
										   @NonNull String permissions[],
										   @NonNull int[] grantResults) {
		List<String> notGranted = new ArrayList<>();
		for (int i = 0; i < grantResults.length; i++) {
			if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
				notGranted.add(permissions[i]);
			}
		}

		if (notGranted.size() < 1) {
			this.onPermissionGranted(permissionRequestCode, true);
		} else {
			this.onPermissionGranted(permissionRequestCode, false);
		}
	}
}
