/*
 * Copyright (c) 2014. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package net.bytle.db.resultSetDiff;


import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * All function to perform a data path diff
 */
public class DataSetDiff {


  private static final int LOSS_EQUALITY = 0;
  private static final int STRICT_EQUALITY = 1;

  private static final String EQUAL = "";
  private static final String PLUS = "+";
  private static final String MIN = "-";
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSetDiff.class);

  private final DataPath firstDataPath;
  private final DataPath secondDataPath;
  private final DataSetDiffResult dataSetDiffResult;
  private Integer keyColumnIndex; // The index of the key column
  private final Locale locale = Locale.getDefault(); // The locale used for String comparison

  private DataPath resultDataPath; // will contains the diff result

  // Represent the type of record inserted in the result
  private final int SOURCE_TYPE = 0;
  private final int TARGET_TYPE = 1;



  // Global variable for easy processing
  private SelectStream selectFirstStream;
  private InsertStream diffInsertStream;
  private SelectStream selectSecondStream;


  /**
   * @param keyColumnIndex - the key column is a column where the diff occurs (bu default, this is the row id). It must be a column where the data is ordered ascendant.
   * @return this data set diff for chaining initialization
   */
  DataSetDiff setKeyColumnIndex(Integer keyColumnIndex) {
    this.keyColumnIndex = keyColumnIndex;
    return this;
  }

  /**
   * @param resultDataPath - the location of the result for this diff (by default, the console)
   * @return this data set diff for chaining initialization
   */
  DataSetDiff setResultDataPath(DataPath resultDataPath) {
    this.dataSetDiffResult.setDataPath(resultDataPath);
    return this;
  }

  static public DataSetDiff of(DataPath firstDataPath, DataPath secondDataPath) {
    return new DataSetDiff(firstDataPath, secondDataPath);
  }

  /*
   * DataSet Diff without a primary key column
   */
  public DataSetDiff(DataPath firstDataPath, DataPath secondDataPath) {

    this.firstDataPath = firstDataPath;
    this.secondDataPath = secondDataPath;
    this.dataSetDiffResult = DataSetDiffResult.of(this);

  }

  /*
   * Compare the current data set with the data set in the parameter
   * and return a data set that contains the differences.
   * If the data set is empty the data set are equals.
   * Throws an exception if the data set are not comparable with the reason
   * The actual data set is the source and the data set given in the parameters is the target
   * @param key: the key column. A key column can only be ordinal (then of type Numeric (Date) or String)
   * The key column can be null, in this case, the row num is considered as the key
   * The key column can not be null, otherwise a DataSet exception is thrown
   * The data set key values are supposed to be in an Ascendant order
   */
  public DataSetDiffResult diff() {



    // Executing the select stream in a thread
    openSelectStream(firstDataPath, secondDataPath);

    // Have the data sets the same structure
    String reason = compareMetaData(firstDataPath, secondDataPath);

    // Can we diff the data set
    if (!reason.equals("")) {

      dataSetDiffResult
        .setDataDefDiff(true)
        .setReason(reason);

    } else {

      // No data def diff
      dataSetDiffResult.setDataDefDiff(false);

      // Building the data definition of the result data path
      resultDataPath.getDataDef()
        .addColumn("+/-") // Add the diff result for the row
        .addColumn("Id");  // Add the id of the row
      TableDef sourceDataDef = firstDataPath.getDataDef();
      // Add the data
      for (int i = 1; i <= sourceDataDef.getColumnDefs().size(); i++) {
        ColumnDef<Object> columnDef = sourceDataDef.getColumnDef(i);
        resultDataPath.getDataDef().addColumn(
          columnDef.getColumnName(),
          columnDef.getDataType().getTypeCode(),
          columnDef.getPrecision(),
          columnDef.getScale(),
          columnDef.getNullable(),
          columnDef.getComment());
      }


      // Get the stream
      diffInsertStream = Tabulars.getInsertStream(resultDataPath);
      selectFirstStream = Tabulars.getSelectStream(firstDataPath);
      selectSecondStream = Tabulars.getSelectStream(secondDataPath);

      // Should we of the next row for the source or the target
      boolean sourceNext = true;
      boolean targetNext = true;

      // Does the source and target set has still row
      Boolean moreSourceRow = false;
      Boolean moreTargetRow = false;

      // Loop until no rows anymore
      while (true) {

        dataSetDiffResult.incrementRowComparison();

        if (sourceNext) {
          moreSourceRow = selectFirstStream.next();
        }
        if (targetNext) {
          moreTargetRow = selectSecondStream.next();
        }

        if (!moreSourceRow && !moreTargetRow) {
          // No more rows to process
          break;
        } else if (moreSourceRow && moreTargetRow) {

          if (keyColumnIndex != null) {

            Object keySourceValue = selectFirstStream.getObject(keyColumnIndex);
            Object keyTargetValue = selectSecondStream.getObject(keyColumnIndex);

            if (keySourceValue == null || keyTargetValue == null) {

              String typeDataSet;
              SelectStream dataset;
              if (keySourceValue == null) {
                dataset = selectFirstStream;
                typeDataSet = "source";
              } else {
                dataset = selectSecondStream;
                typeDataSet = "target";
              }
              throw new RuntimeException("The value of the primary key column (col: " + keyColumnIndex + ", row:" + dataset.getRow() + ") in the " + typeDataSet + " data set can not be null");
            }

            if (keySourceValue.equals(keyTargetValue)) {

              Boolean result = this.compareAndAddRowData();
              dataSetDiffResult.addLineComparisonResult(result);
              targetNext = true;
              sourceNext = true;

            } else if (keySourceValue.toString().compareTo(keyTargetValue.toString()) > 0) {

              dataSetDiffResult.addLineComparisonResult(true);
              addRowData(TARGET_TYPE);
              targetNext = true;
              sourceNext = false;

            } else {

              //less than
              dataSetDiffResult.addLineComparisonResult(true);
              addRowData(SOURCE_TYPE);
              targetNext = false;
              sourceNext = true;

            }


          } else {

            // No primary key
            Boolean result = this.compareAndAddRowData();
            dataSetDiffResult.addLineComparisonResult(result);
            targetNext = true;
            sourceNext = true;

          }
        } else {

          // There is still a source row of a target row
          dataSetDiffResult.addLineComparisonResult(true);
          if (moreSourceRow) {

            addRowData(SOURCE_TYPE);
            targetNext = false;
            sourceNext = true;

          } else {

            addRowData(TARGET_TYPE);
            targetNext = true;
            sourceNext = false;

          }
        }
      }
    }


    LOGGER.info("Counter info: number of row in the diff file: (" + dataSetDiffResult.getRows() + ")");
    LOGGER.info("Counter info: number of diff loop: (" + dataSetDiffResult.getNumberOfRowComparison() + ")");
    return dataSetDiffResult;

  }

  /**
   * Opening the select stream in a thread (ie execute a request to get the data if needed)
   *
   * @param firstDataPath
   * @param secondDataPath Opening has the meaning of executing a request if the select stream need it
   *                       (example in the case of a query
   */
  private void openSelectStream(DataPath firstDataPath, DataPath secondDataPath) {

    OpenSelectStreamThread openSelectStreamThread1 = new OpenSelectStreamThread(firstDataPath);
    OpenSelectStreamThread openSelectStreamThread2 = new OpenSelectStreamThread(secondDataPath);
    Thread t1 = new Thread(openSelectStreamThread1);
    Thread t2 = new Thread(openSelectStreamThread2);
    t1.start();
    t2.start();
    try {
      t1.join();
    } catch (InterruptedException e) {
      String msg = "An exception has occurred during the execution of the first data path (" + firstDataPath + ")";
      LOGGER.error(msg, e);
      throw new RuntimeException(msg, e);
    }
    try {
      t2.join();
    } catch (InterruptedException e) {
      String msg = "An exception has occurred during the execution of the second data path (" + secondDataPath + ")";
      LOGGER.error(msg, e);
      throw new RuntimeException(msg, e);
    }
    // Check for any errors in the thread
    if (openSelectStreamThread1.isError()) {
      String msg = "An exception has occurred during the execution of the first data path (" + firstDataPath + ") ";
      LOGGER.error(msg, openSelectStreamThread1.getError());
      throw new RuntimeException(msg, openSelectStreamThread1.getError());
    }
    if (openSelectStreamThread2.isError()) {
      String msg = "An exception has occurred during the execution of the first data path (" + secondDataPath + ") ";
      LOGGER.error(msg, openSelectStreamThread2.getError());
      throw new RuntimeException(msg, openSelectStreamThread2.getError());
    }
  }

  private void addRowData(int type) {
    addRowData(type, null, null);
  }

  private void addRowData(int type, String equalMinPlus, List<Diff> columnPositionWithDiff) {

    if (resultDataPath != null) {

      dataSetDiffResult.addRow();

      SelectStream dataSet;
      if (type == SOURCE_TYPE) {
        dataSet = selectFirstStream;
        if (equalMinPlus == null) {
          equalMinPlus = MIN;
        }

      } else {
        dataSet = selectSecondStream;
        if (equalMinPlus == null) {
          equalMinPlus = PLUS;
        }
      }


      List csvRow = new ArrayList();

      // Min Plus Column
      csvRow.add(equalMinPlus);

      // Key Column
      if (keyColumnIndex != null) {
        csvRow.add(keyColumnIndex);
      } else {
        csvRow.add(dataSet.getRow());
      }

      List<Integer> columnPositions = new ArrayList<>();
      if (columnPositionWithDiff != null) {
        columnPositionWithDiff.forEach(diff -> columnPositions.add(diff.getPosition()));
      }

      for (int i = 1; i <= dataSet.getSelectDataDef().getColumnDefs().size(); i++) {
        Object object = dataSet.getObject(i);

        if (columnPositions.contains(i)) {
          if (object != null) {
            object = "***" + object + "***";
          } else {
            object = "***(null)***";
          }
        }

        csvRow.add(object);
      }

      diffInsertStream.insert(csvRow);


    }


  }

  /*
   * Compare a whole row and
   * @return
   */
  private Boolean compareAndAddRowData() {

    Boolean diffFound = false;
    List<Diff> columnPositionWithDiff = new ArrayList<>();
    for (int i = 1; i <= selectFirstStream.getSelectDataDef().getColumnDefs().size(); i++) {

      String cellCoordinates = "Cell(Row,Col)(" + selectFirstStream.getRow() + "," + i + ")";

      Object sourceDataPoint = selectFirstStream.getObject(i);
      Object targetDataPoint = selectSecondStream.getObject(i);
      if (sourceDataPoint != null) {
        if (targetDataPoint == null) {
          diffFound = true;
        } else {
          if (!sourceDataPoint.equals(targetDataPoint)) {

            diffFound = true;
            Diff diff = new Diff(i);
            if (sourceDataPoint.getClass().equals(Double.class)) {
              boolean equalWithFloat = (new Float((Double) sourceDataPoint).equals(new Float((Double) targetDataPoint)));
              if (equalWithFloat) {
                diff.setType(LOSS_EQUALITY);
                diffFound = false;
              }
            }
            columnPositionWithDiff.add(diff);

            LOGGER.trace(cellCoordinates + ", Diff Found !: " + sourceDataPoint + "," + targetDataPoint);

          } else {

            LOGGER.trace(cellCoordinates + ", No Diff: " + sourceDataPoint + "," + targetDataPoint);

          }
        }
      } else {
        if (targetDataPoint != null) {
          diffFound = true;
          LOGGER.trace(cellCoordinates + ", Diff: (null)," + targetDataPoint);
        } else {
          LOGGER.trace(cellCoordinates + ", No Diff: (null),(null)");
        }
      }

    }

    if (diffFound) {
      addRowData(SOURCE_TYPE, null, columnPositionWithDiff);
      addRowData(TARGET_TYPE, null, columnPositionWithDiff);
    } else {
      addRowData(SOURCE_TYPE, EQUAL, null);
    }
    return diffFound;

  }

  /**
   * If the metadata are equals, this function return null. Otherwise return the reason
   *
   * @param firstDataPath
   * @param secondDataPath
   * @return
   */
  static String compareMetaData(DataPath firstDataPath, DataPath secondDataPath) {

    StringBuilder reason = new StringBuilder();

    // Length
    int sourceSize = firstDataPath.getDataDef().getColumnDefs().size();
    int targetSize = secondDataPath.getDataDef().getColumnDefs().size();
    if (sourceSize != targetSize) {
      reason.append("The number of columns are not equals. The source data set has ");
      reason.append(sourceSize);
      reason.append(" columns, and the target data set has ");
      reason.append(targetSize);
      reason.append(" columns.");
      reason.append(System.getProperty("line.separator"));
    }


    // Type
    for (int i = 0; i < sourceSize; i++) {
      ColumnDef sourceColumn = firstDataPath.getDataDef().getColumnDef(i);
      ColumnDef targetColumn = secondDataPath.getDataDef().getColumnDef(i);
      if (sourceColumn.getDataType().getTypeCode() != targetColumn.getDataType().getTypeCode()) {
        reason.append("The type column of the column (");
        reason.append(i);
        reason.append(") are not equals. The source column (");
        reason.append(sourceColumn.getColumnName());
        reason.append(") has the type (");
        reason.append(sourceColumn.getDataType().getTypeName());
        reason.append(") whereas the target column (");
        reason.append(targetColumn.getColumnName());
        reason.append(") has the column type (");
        reason.append(targetColumn.getDataType().getTypeName());
        reason.append(").");
        reason.append(System.getProperty("line.separator"));
      }
    }
    return reason.toString();

  }


  private class Diff {

    private Integer position;
    private Integer type = STRICT_EQUALITY;

    public Diff(int position) {
      this.position = position;
    }


    void setType(Integer type) {
      this.type = type;
    }

    public Integer getType() {
      return type;
    }

    public Integer getPosition() {
      return position;
    }
  }

}
