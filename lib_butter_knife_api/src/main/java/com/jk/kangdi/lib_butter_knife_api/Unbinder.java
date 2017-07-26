package com.jk.kangdi.lib_butter_knife_api;

import android.support.annotation.UiThread;

/** An unbinder contract that will unbind views when called. */
public interface Unbinder {
  @UiThread
  void unbind();

  Unbinder EMPTY = new Unbinder() {
    @Override public void unbind() { }
  };
}
