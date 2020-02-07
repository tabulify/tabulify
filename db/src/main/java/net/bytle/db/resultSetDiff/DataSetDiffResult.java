package net.bytle.db.resultSetDiff;

import net.bytle.db.spi.DataPath;

public class DataSetDiffResult {
  private final DataSetDiff dataSetDiff;
  private String reason; // if there is a diff, this is a summary of the reason


  private int numberOfRowComparison = 0; // To Feedback the number of loop performed

  // Counter
  private int numberOfSameRows = 0;
  private int numberOfDiffRows = 0;
  private int numberOfRows = 0;

  private DataPath dataPath;

  // Do we have the same data definition
  private boolean isDataDefDiff;



  public DataSetDiffResult(DataSetDiff dataSetDiff) {
    this.dataSetDiff = dataSetDiff;
  }

  public static DataSetDiffResult of(DataSetDiff dataSetDiff) {
    return new DataSetDiffResult(dataSetDiff);
  }


  public DataSetDiffResult areLinesEquals(Boolean dataSetDiffFound) {
    if (dataSetDiffFound){
      this.numberOfSameRows++;
    } else {
      this.numberOfDiffRows++;
    }

    return this;
  }

  public DataSetDiffResult setReason(String reason) {
    this.reason = reason;
    return this;
  }

  public DataSetDiffResult setDataPath(DataPath dataPath) {
    this.dataPath = dataPath;
    return this;
  }

  public DataSetDiffResult incrementRowComparison() {
    this.numberOfRowComparison++;
    return this;
  }

  public DataSetDiffResult setDataDefDiffFound(boolean b) {
    this.isDataDefDiff = b;
    return this;
  }

  public int getNumberOfRowComparison() {
    return numberOfRowComparison;
  }

  public int getRows() {
    return this.numberOfRows;
  }

  public DataSetDiffResult rowAdded() {
    this.numberOfRows++;
    return this;
  }

  public boolean areEquals() {
    return numberOfDiffRows == 0 && isDataDefEquals();
  }

  public boolean isDataDefEquals() {
    return !isDataDefDiff;
  }

  public DataPath getDataPath() {
    return this.dataPath;
  }
}
