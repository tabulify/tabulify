package com.tabulify.flow.operation;


import com.tabulify.spi.DataPath;

/**
 * The execution mode
 */
public enum ExecutionMode {

  /**
   * The request is executed and a loop over the result is done
   * (ie {@link com.tabulify.stream.SelectStream} is used
   */
  TRANSFER,
  /**
   * Count will send a count request
   * (ie select count from my query)
   * (ie {@link DataPath#getCount()} is used
   */
  LOAD;


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}
