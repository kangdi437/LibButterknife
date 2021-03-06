package com.jk.kangdi;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

/**
 * Represents an ID of an Android resource.
 */
final class Id {
  private static final ClassName ANDROID_R = ClassName.get("android", "R");

  final String value;
  final CodeBlock code;
  final boolean qualifed;

  Id(String value) {
    this.value = value;
    this.code = CodeBlock.of("$L", value);
    this.qualifed = false;
  }

  Id(String value, ClassName className, String resourceName) {
    this.value = value;
    this.code = className.topLevelClassName().equals(ANDROID_R)
      ? CodeBlock.of("$L.$N", className, resourceName)
      : CodeBlock.of("$T.$N", className, resourceName);
    this.qualifed = true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Id id = (Id) o;

    if (qualifed != id.qualifed) return false;
    if (value != null ? !value.equals(id.value) : id.value != null) return false;
    return code != null ? code.equals(id.code) : id.code == null;

  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (qualifed ? 1 : 0);
    return result;
  }

  @Override public String toString() {
    throw new UnsupportedOperationException("Please use value or code explicitly");
  }
}
