package com.jk.kangdi;

final class QualifiedId {
  final String packageName;
  final String id;

  QualifiedId(String packageName, String id) {
    this.packageName = packageName;
    this.id = id;
  }

  @Override public String toString() {
    return "QualifiedId{packageName='" + packageName + "', id=" + id + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QualifiedId that = (QualifiedId) o;

    if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null)
      return false;
    return id != null ? id.equals(that.id) : that.id == null;

  }

  @Override
  public int hashCode() {
    int result = packageName != null ? packageName.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    return result;
  }
}
