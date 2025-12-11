package com.tabulify.type;

import java.util.List;
import java.util.Map;

/**
 * Static Utility that improves {@link java.util.Objects}
 *
 */
public class ObjectStatics {

  /**
   * Utility function to see if an object is empty or not
   */
  public static Boolean isEmpty(Object object) {
    if (object instanceof String) {
      return ((String) object).isEmpty();
    }
    if (object instanceof List) {
      return ((List<?>) object).isEmpty();
    }
    if (object instanceof Map) {
      return ((Map<?, ?>) object).isEmpty();
    }
    if (object.getClass().isArray()) {
      return ((Object[]) object).length == 0;
    }
    return false;
  }

}
