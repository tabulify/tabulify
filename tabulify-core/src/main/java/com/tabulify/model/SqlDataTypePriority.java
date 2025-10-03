package com.tabulify.model;

public enum SqlDataTypePriority {

  /**
   * Types that comes from the driver
   * and alias
   */
  DEFAULT(0),
  /**
   * The priority of all {@link SqlDataTypeAnsi}
   */
  STANDARD(1),
  /**
   * The priority between type class of all {@link SqlDataTypeAnsi}
   * It's just to let {@link SqlDataTypeAnsi#CHARACTER_VARYING} and {@link SqlDataTypeAnsi#INTEGER}
   * win if a string/integer column is asked
   */
  STANDARD_TOP(2),
  /**
   * If we want to top out a priority
   */
  TOP(3);

  private final int priority;

  SqlDataTypePriority(int priority) {
    this.priority = priority;
  }

  public int getValue() {
    return this.priority;
  }
}
