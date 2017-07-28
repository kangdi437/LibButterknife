package com.jk.kangdi;

import com.jk.kangdi.internal.ListenerClass;
import com.jk.kangdi.internal.ListenerMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Target(METHOD)
@Retention(CLASS)
@ListenerClass(
    targetType = "android.view.View",
    setter = "setOnClickListener",
    type = "com.jk.kangdi.lib_butter_knife_api.DebouncingOnClickListener",
    method = @ListenerMethod(
        name = "doClick",
        parameters = "android.view.View"
    )
)
public @interface OnClick {
  /** View IDs to which the method will be bound. */
  String[] value() default { "-1"};
}
