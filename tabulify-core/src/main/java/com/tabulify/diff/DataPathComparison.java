package com.tabulify.diff;


import com.tabulify.spi.DataPath;

public class DataPathComparison {

  private final DataPath source;
  private final DataPath target;

  enum level {
    ATTRIBUTES,
    STRUCTURE,
    DATA
  }


  public DataPathComparison(DataPath source, DataPath target) {
    this.source = source;
    this.target = target;
  }

  /**
   * Compare the current data set with the data set in the parameter
   * and return a data set that contains the differences.
   * If the data set is empty the data set are equals.
   * Throws an exception if the data set are not comparable with the reason
   * The actual data set is the source and the data set given in the parameters is the target
   * <p>
   * A key column can only be ordinal (then of type Numeric (Date) or String)
   * The key column can be null, in this case, the row num is considered as the key
   * The key column can not be null, otherwise a DataSet exception is thrown
   * The data set key values are supposed to be in an Ascendant order
   */
  public DataPathComparison compare() {


    // Have the data sets the same structure
    compareStructure();

    throw new RuntimeException("To finish");


  }

  /**
   * If the metadata are equals, the list of diff should be zero
   *
   * @return the comparison
   */
  public DataPathDiffResult compareStructure() {

    return source
      .getOrCreateRelationDef()
      .diff(target);

  }

}
