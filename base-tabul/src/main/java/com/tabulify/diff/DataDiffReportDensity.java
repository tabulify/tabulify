package com.tabulify.diff;

/**
 * The density (called the context in diff)
 */
public enum DataDiffReportDensity {

  /**
   * All records are in the diff results report
   * (Ie the context is added)
   */
  DENSE,
  /**
   * Only differences are shown in the results report
   * * records deleted,
   * * records added
   * * on records updated
   * * drivers cell
   * * cell with diff
   * * equal cells are null
   */
  SPARSE

}
