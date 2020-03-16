package net.bytle.type;

/**
 * Boolean(static) function
 */
public class Booleans {

  /**
   * A wrapper around {@link Boolean#parseBoolean(String)}
   * @param s
   * @return
   */
  Boolean fromString(String s) {
    return Boolean.parseBoolean(s);
  }

}
