package com.jk.kangdi.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by admin on 2017/7/29.
 */

@Target(METHOD)
@Retention(CLASS)
public @interface Convert {
    String itemLayoutId() default "";
    String filedName() default "";
}
