package com.tabulify.diff;

public enum DataDiffEqualityType {
  /**
   * ie equal after cast
   */
  LOSS,
  /**
   * strict equality
   * (ie equals function)
   */
  STRICT
}
