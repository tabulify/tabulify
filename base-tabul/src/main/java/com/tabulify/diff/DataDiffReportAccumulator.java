package com.tabulify.diff;

/**
 * The report that accumulates the changes
 */
public enum DataDiffReportAccumulator {

  /**
   * Unified report that accumulates at the record level
   */
  UNIFIED,
  /**
   * Summary report only (no accumulation of changes)
   */
  NONE,
  /**
   * Report that accumulates at the cell level
   */
  CELL

}
