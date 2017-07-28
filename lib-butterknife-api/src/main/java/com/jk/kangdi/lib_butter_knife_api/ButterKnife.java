package com.jk.kangdi.lib_butter_knife_api;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by JINKANG on 2017/7/26.
 */

public class ButterKnife {

    private static boolean debug = false;

    @VisibleForTesting
    static final Map<Class<?>, Constructor<? extends Unbinder>> BINDINGS = new LinkedHashMap<>();

    @NonNull
    @UiThread
    public static Unbinder bind(@NonNull Activity target) {
        View sourceView = target.getWindow().getDecorView();
        return createBinding(target, sourceView);
    }

    @NonNull @UiThread
    public static Unbinder bind(@NonNull View target) {
        return createBinding(target, target);
    }

    @NonNull @UiThread
    public static Unbinder bind(@NonNull Dialog target) {
        View sourceView = target.getWindow().getDecorView();
        return createBinding(target, sourceView);
    }

    private static Unbinder createBinding(@NonNull Object target, @NonNull View source) {
        Class<?> targetClass = target.getClass();
        if (debug) Log.d(TAG, "Looking up binding for " + targetClass.getName());
        Constructor<? extends Unbinder> constructor = findBindingConstructorForClass(targetClass);

        if (constructor == null) {
            return Unbinder.EMPTY;
        }

        //noinspection TryWithIdenticalCatches Resolves to API 19+ only type.
        try {
            return constructor.newInstance(target, source);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create binding instance.", cause);
        }
    }

    @Nullable
    @CheckResult
    @UiThread
    private static Constructor<? extends Unbinder> findBindingConstructorForClass(Class<?> cls) {
        Constructor<? extends Unbinder> bindingCtor = BINDINGS.get(cls);
        if (bindingCtor != null) {
            if (debug) Log.d(TAG, "HIT: Cached in binding map.");
            return bindingCtor;
        }
        String clsName = cls.getName();
        if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
            if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return null;
        }
        try {
            Class<?> bindingClass = cls.getClassLoader().loadClass(clsName + "_ViewBinding");
            //noinspection unchecked
            bindingCtor = (Constructor<? extends Unbinder>) bindingClass.getConstructor(cls, View.class);
            if (debug) Log.d(TAG, "HIT: Loaded binding class and constructor.");
        } catch (ClassNotFoundException e) {
            if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            bindingCtor = findBindingConstructorForClass(cls.getSuperclass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
        }
        BINDINGS.put(cls, bindingCtor);
        return bindingCtor;
    }

}
