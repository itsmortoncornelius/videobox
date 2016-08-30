package de.handler.mobile.android.videobox;

import android.Manifest;
import android.support.annotation.StringDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@StringDef({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
@interface Permission {
}
