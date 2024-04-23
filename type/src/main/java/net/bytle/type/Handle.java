package net.bytle.type;

import java.util.Objects;

/**
 * A handle is a unique value.
 * If the empty string is passed, the value become null
 * otherwise the uniqueness is broken
 */
public class Handle {


  private static final int MAX_LENGTH = 32;
  private final String handle;

  public Handle(String s) throws HandleCastException {
    if (s == null) {
      handle = null;
      return;
    }
    if (s.isBlank()) {
      handle = null;
      return;
    }
    try {
      handle = DnsName.create(s).toStringWithoutRoot();
    } catch (DnsCastException e) {
      throw new HandleCastException("The value (" + s + ") is a not a valid handle", e);
    }
    if (handle.length() > MAX_LENGTH) {
      throw new HandleCastException("The value (" + s + ") is too long (max "+MAX_LENGTH+")");
    }

  }

  public static Handle ofFailSafe(String listHandleColumn) {
      try {
          return new Handle(listHandleColumn);
      } catch (HandleCastException e) {
          throw new RuntimeException(e);
      }
  }

  public String getValueOrNull() {
    return handle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Handle handle1 = (Handle) o;
    return Objects.equals(handle, handle1.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(handle);
  }
}
