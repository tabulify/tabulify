package com.tabulify.spi;

/**
 * The tabular type defines if the tabular represents
 * * pure static data (data)
 * * or log of command (exit status, message)
 * <p>
 * Usage: It's used originally in the exec command to define
 * the default type of result
 * For instance,
 * * a sql insert will perform a count
 * * while a sql insert will perform a transfer (ie store the results)
 */
public enum TabularType {

  /**
   * Data
   */
  DATA,
  /**
   * Log of a command
   * (ie exit status, error message)
   */
  COMMAND;


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}
