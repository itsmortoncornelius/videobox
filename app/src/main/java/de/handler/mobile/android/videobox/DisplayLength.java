package de.handler.mobile.android.videobox;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static android.support.design.widget.Snackbar.LENGTH_INDEFINITE;
import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})
@interface DisplayLength {
	int length() default LENGTH_SHORT;
}
