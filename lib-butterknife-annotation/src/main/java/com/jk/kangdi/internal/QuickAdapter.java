package com.jk.kangdi.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by admin on 2017/7/29.
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface QuickAdapter {
    String value();
    String emptyResId() default "";
}
