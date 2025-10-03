package com.tabulify.tabul;

public enum TabulDataTransferCommandType {
  /**
   * The fill transfer has other option
   */
  FILL,
  /**
   * concat is the only one with multiple source
   * to concatenate in the last one
   */
  CONCAT,
  /**
   * Transfer from one source to a target
   */
  DEFAULT
}
