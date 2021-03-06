package de.handler.mobile.android.videobox;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@IntDef({
		PermissionRequestCode.RequestCodes.REQUEST_CODE_PERMISSION_CAMERA,
		PermissionRequestCode.RequestCodes.REQUEST_CODE_PERMISSION_RECORD_AUDIO})
@interface PermissionRequestCode {
	interface RequestCodes {
		int REQUEST_CODE_PERMISSION_CAMERA = 201;
		int REQUEST_CODE_PERMISSION_RECORD_AUDIO = 202;
	}
}
