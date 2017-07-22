package de.handler.mobile.android.videobox;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@IntDef({
		CameraStates.Definition.START,
		CameraStates.Definition.STOP})
@interface CameraStates {
	interface Definition {
		int START = 401;
		int STOP = 402;
	}
}
