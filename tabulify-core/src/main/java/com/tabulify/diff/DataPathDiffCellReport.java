package com.tabulify.diff;

import com.tabulify.spi.DataPath;

/**
 * Not finished
 */
public class DataPathDiffCellReport implements DataPathDiffReport {

  private final DataPathDiffResult result;

  public DataPathDiffCellReport(DataPathDiffResult dataPathDiffResult, DataPath resultDataPath) {
    this.result = dataPathDiffResult;
  }

  @Override
  public DataPath getDataPath() {
    throw new UnsupportedOperationException("The cell report is not supported yet.");
  }

  @Override
  public void insertResultRecord(DataDiffCells cells, DataPathDiffStatus diffStatus) {
    throw new UnsupportedOperationException("The cell report is not supported yet.");
  }

}
