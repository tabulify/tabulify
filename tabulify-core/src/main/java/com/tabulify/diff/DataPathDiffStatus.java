package com.tabulify.diff;

/**
 * The operation to perform on source to get to the target
 */
public enum DataPathDiffStatus {

  DELETE("-", "<", "Not found in target (found in source)"),
  ADD("+", ">", "Added in target (not found in source)"),
  VALUE("+-", "<>", "value modification"),
  /**
   * No change was the empty string but because we now print the empty string
   * as <empty> to see a diff with <null> it is ugly
   */
  NO_CHANGE("=", "=", "No Change"),
  ;

  private final String unifiedSymbol;
  private final String description;
  private final String traditionalSymbol;

  DataPathDiffStatus(String unifiedSymbol, String traditionalSymbol, String s) {

    this.unifiedSymbol = unifiedSymbol;
    this.traditionalSymbol = traditionalSymbol;
    this.description = s;

  }

  public String getMathematicsSymbol() {
    return unifiedSymbol;
  }

  public String getDescription() {
    return description;
  }

  public String getTraditionalSymbol() {
    return traditionalSymbol;
  }
}
