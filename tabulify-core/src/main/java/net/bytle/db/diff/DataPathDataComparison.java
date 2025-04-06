

package net.bytle.db.diff;


import net.bytle.db.DbLoggers;
import net.bytle.db.Tabular;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectException;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.exception.NoColumnException;
import net.bytle.log.Log;
import net.bytle.type.Key;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.diff.DataComparisonAttribute.*;
import static net.bytle.db.diff.DataPathDataComparison.rowSource.BOTH_DATA_PATH;
import static net.bytle.db.diff.DataPathDataComparison.rowSource.SOURCE_DATA_PATH;
import static net.bytle.db.diff.DataPathDataComparison.rowSource.TARGET_DATA_PATH;

/**
 * All function to perform a data path comparison
 * on data level
 * <p>
 * This is set apart of the {@link DataPathComparison}
 * because we make also a comparison on the data of the structure (ie columns)
 */
public class DataPathDataComparison {

  public static final String BOLD_CHARACTERS = "***";
  private final Tabular tabular;


  /**
   * The result data path of the data comparison
   */
  private DataPath resultDataPath;


  /**
   * The number of same rows
   */
  private int numberOfSameRows;
  /**
   * The number of diff rows
   */
  private int numberOfDiffRows;

  /**
   * The id of the returned data
   */
  private int compRecordId = 1;

  public Integer getDiffCount() {
    if (!hasComparisonRun) {
      compareData();
    }
    return numberOfDiffRows;
  }


  enum Strictness {
    LOSS_EQUALITY,
    STRICT_EQUALITY
  }


  private static final String EQUAL = "";
  private static final String PLUS = "+";
  private static final String MIN = "-";
  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

  private final DataPath source;
  private final DataPath target;

  private final List<Integer> driverColumnsPosition = new ArrayList<>(); // The index of the key column

  enum rowSource {
    SOURCE_DATA_PATH,
    TARGET_DATA_PATH,
    BOTH_DATA_PATH
  }


  /**
   * A blocker to compare only once
   */
  private boolean hasComparisonRun = false;

  /**
   * Line counter
   */
  private Long lineSourceId = 0L;
  private Long lineTargetId = 0L;


  /**
   * @param columnNames - the column where the diff occurs (by default, this is the row id). It must be a column where the data is ordered ascendant.
   * @return this data set diff for chaining initialization
   */
  public DataPathDataComparison setUniqueColumns(String... columnNames) {
    for (String columnName : columnNames) {
      ColumnDef columnDef;
      try {
        columnDef = this.source.getOrCreateRelationDef().getColumnDef(columnName);
        this.driverColumnsPosition.add(columnDef.getColumnPosition());
      } catch (NoColumnException e) {
        throw new IllegalStateException("The column (" + columnName + ") was not found in the source (" + source + ") and cannot be used as a unique column (driver column) for the comparison.");
      }
    }
    return this;
  }

  /**
   * @param resultDataPath - the location of the result for this diff (by default, the console)
   * @return this data set diff for chaining initialization
   */
  public DataPathDataComparison setResultDataPath(DataPath resultDataPath) {
    this.resultDataPath = resultDataPath;
    return this;
  }

  static public DataPathDataComparison create(DataPath leftDataPath, DataPath rightDataPath) {
    return new DataPathDataComparison(leftDataPath, rightDataPath);
  }


  public DataPathDataComparison(DataPath source, DataPath target) {

    this.source = source;
    this.target = target;
    this.tabular = source.getConnection().getTabular();

  }


  /**
   * Compare the data
   * There is no columns comparisons to avoid reflective comparison.
   * The passed source and target should have the same number of columns, if not, an error is throw
   *
   * @return the comparison
   */
  public DataPathDataComparison compareData() {

    if (hasComparisonRun) {
      throw new RuntimeException("This comparison has already run, you can't run it again");
    } else {
      hasComparisonRun = true;
    }

    /**
     * High columns check
     */
    int sourceColumnSize = source.getOrCreateRelationDef().getColumnsSize();
    int targetColumnSize = target.getOrCreateRelationDef().getColumnsSize();
    if (sourceColumnSize != targetColumnSize) {
      throw new RuntimeException("We can't compare the data of the content resource because the source resource (" + source + ") has (" + sourceColumnSize + ") columns while the target resource (" + target + ") has (" + targetColumnSize + ") columns.");
    }
    if (sourceColumnSize == 0) {
      throw new RuntimeException("We can't compare the data of the content resource because the source resource (" + source + ") has no columns.");
    }

    Integer rowIdColumnPosition = null;
    if (driverColumnsPosition.size() > 0) {
      if (driverColumnsPosition.size() > 1) {
        throw new RuntimeException("We don't support comparing data resource with more than one lookup columns");
      } else {
        rowIdColumnPosition = driverColumnsPosition.get(0);
      }
    }

    /**
     * The result data resource
     */
    if (resultDataPath == null) {
      resultDataPath = tabular
        .getMemoryDataStore()
        .getAndCreateRandomDataPath()
        .setLogicalName("comparison_" + source.getLogicalName() + "_" + target.getLogicalName())
        .setDescription(
          "Comparison result between source (" + source + ") and target (" + target + ")." +
            Strings.EOL + "Values that are not equals are encapsulated with the characters `" + BOLD_CHARACTERS + "`."
        );
    }
    /*
     * To avoid conflict with the column name of the
     * resource to compare the comparison columns
     * have the `comp` prefix
     */
    resultDataPath.createRelationDef()
      .addColumn(KeyNormalizer.create(COMP_ID).toSqlCase())  // Add the id of the row
      .addColumn(Key.toColumnName(COMP_ORIGIN)) // The origin of the data
      .addColumn(Key.toColumnName(COMP_COMMENT))
      .addColumn(Key.toColumnName(COMP_DIFF_ID))
    ;  //

    /*
     * Add the line id if there is no id in the data
     */
    if (driverColumnsPosition.size() == 0) {
      resultDataPath
        .getOrCreateRelationDef()
        .addColumn(Key.toColumnName(COMP_ORIGIN_ID));
    }

    RelationDef sourceDataDef = source.getOrCreateRelationDef();
    // Add the data
    for (int i = 1; i <= sourceDataDef.getColumnsSize(); i++) {
      ColumnDef columnDef = sourceDataDef.getColumnDef(i);
      resultDataPath.getOrCreateRelationDef()
        .addColumn(
          columnDef.getColumnName(),
          columnDef.getDataType().getTypeCode(),
          columnDef.getPrecision(),
          columnDef.getScale(),
          true, // Optional because it may not exist in the target
          columnDef.getComment());
    }
    Tabulars.createIfNotExist(resultDataPath);


    // Get the stream
    try (
      InsertStream resultStream = resultDataPath.getInsertStream();
      SelectStream firstStream = source.getSelectStream();
      SelectStream secondStream = target.getSelectStream()
    ) {

      // Should we of the next row for the source or the target
      boolean sourceNext = true;
      boolean targetNext = true;

      // Does the source and target set has still row
      boolean moreSourceRow = false;
      boolean moreTargetRow = false;


      // Loop until no rows anymore
      while (true) {


        if (sourceNext) {

          moreSourceRow = firstStream.next();
          if (moreSourceRow) {
            lineSourceId++;
          }
        }
        if (targetNext) {
          moreTargetRow = secondStream.next();
          if (moreTargetRow) {
            lineTargetId++;
          }
        }

        if (!moreSourceRow && !moreTargetRow) {
          // No more rows to process
          break;
        } else if (moreSourceRow && moreTargetRow) {

          if (rowIdColumnPosition != null) {

            Object keySourceValue = firstStream.getObject(rowIdColumnPosition);
            Object keyTargetValue = secondStream.getObject(rowIdColumnPosition);

            if (keySourceValue == null || keyTargetValue == null) {

              String typeDataSet;
              SelectStream dataset;
              if (keySourceValue == null) {
                dataset = firstStream;
                typeDataSet = "source";
              } else {
                dataset = secondStream;
                typeDataSet = "target";
              }
              throw new RuntimeException("The value of the primary key column (col: " + rowIdColumnPosition + ", row:" + dataset.getRow() + ") in the " + typeDataSet + " data set can not be null");
            }

            if (keySourceValue.equals(keyTargetValue)) {

              compareAndAddRowData(firstStream, secondStream, resultStream);
              targetNext = true;
              sourceNext = true;

            } else if (keySourceValue.toString().compareTo(keyTargetValue.toString()) > 0) {

              areLinesEquals(false);
              addRowData(resultStream, secondStream, TARGET_DATA_PATH, null);
              targetNext = true;
              sourceNext = false;

            } else {

              //less than
              areLinesEquals(false);
              addRowData(resultStream, firstStream, SOURCE_DATA_PATH, null);
              targetNext = false;
              sourceNext = true;

            }


          } else {

            // No primary key
            compareAndAddRowData(firstStream, secondStream, resultStream);

          }
        } else {

          // There is still a source row of a target row
          areLinesEquals(false);
          if (moreSourceRow) {

            addRowData(resultStream, firstStream, SOURCE_DATA_PATH, null);
            targetNext = false;
            sourceNext = true;

          } else {

            addRowData(resultStream, secondStream, TARGET_DATA_PATH, null);
            targetNext = true;
            sourceNext = false;

          }
        }
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  private void areLinesEquals(Boolean result) {

    if (result) {
      numberOfSameRows++;
    } else {
      numberOfDiffRows++;
    }


  }

  /**
   * Add a row in the diff result data
   * <p>
   * There is only 4 cases:
   * <p>
   * A row is present only in the first data path, one insert of
   * * selectStream is the first stream
   * * type {@link rowSource#SOURCE_DATA_PATH}
   * <p>
   * A row is present only in the second data path, one insert of
   * * selectStream is the second stream
   * * type {@link rowSource#TARGET_DATA_PATH}
   * <p>
   * A row is the same, one insert of
   * * selectStream is the first or second stream
   * * type {@link rowSource#BOTH_DATA_PATH}
   * * no coordinates cell
   * <p>
   * A row is not the same: two inserts
   * <p>
   * First:
   * * first selectStream
   * * type {@link rowSource#SOURCE_DATA_PATH}
   * * coordinates cell of the diff
   * and
   * Second:
   * * second selectStream
   * * type {@link rowSource#TARGET_DATA_PATH}
   * * coordinates cell of the diff
   *
   * @param resultStream            - the stream of the result data path
   * @param selectStream            - the stream where the data comes from
   * @param type                    - the type of insertion (only present in source, only present in target, or present in both)
   * @param cellWithDiffCoordinates - the coordinates of the cells with a diff
   */
  private void addRowData(InsertStream resultStream, SelectStream selectStream, rowSource type, List<CellDiff> cellWithDiffCoordinates) {

    String reason;
    String origin;
    switch (type) {
      case SOURCE_DATA_PATH:
        origin = "Source";
        if (cellWithDiffCoordinates == null || cellWithDiffCoordinates.size() == 0) {
          reason = "Record not found in target";
        } else {
          reason = "Value Diff (";
          reason += cellWithDiffCoordinates
            .stream()
            .map(e -> source.getOrCreateRelationDef().getColumnDef(e.getColId()).getColumnName())
            .collect(Collectors.joining(", "));
          reason += ")";
        }
        break;
      case TARGET_DATA_PATH:
        origin = "Target";
        if (cellWithDiffCoordinates == null || cellWithDiffCoordinates.size() == 0) {
          reason = "Record not found in source";
        } else {
          reason = "Value Diff (";
          reason += cellWithDiffCoordinates
            .stream()
            .map(e -> source.getOrCreateRelationDef().getColumnDef(e.getColId()).getColumnName())
            .collect(Collectors.joining(", "));
          reason += ")";
        }
        break;
      case BOTH_DATA_PATH:
        reason = "";
        origin = "";
        break;
      default:
        throw new RuntimeException("Type of insertion is unknown");
    }


    List<Object> row = new ArrayList<>();

    row.add(compRecordId++);

    // Min Plus Column
    row.add(origin);
    // Reason
    row.add(reason);
    if (type != BOTH_DATA_PATH) {
      row.add(numberOfDiffRows);
    } else {
      row.add(null);
    }

    if (driverColumnsPosition.size() == 0) {
      switch (type) {
        case BOTH_DATA_PATH:
        case SOURCE_DATA_PATH:
          row.add(lineSourceId);
          break;
        case TARGET_DATA_PATH:
          row.add(lineTargetId);
          break;
        default:
          throw new RuntimeException("Type of insertion is unknown");
      }
    }

    List<Integer> columnPositions = new ArrayList<>();
    if (cellWithDiffCoordinates != null) {
      cellWithDiffCoordinates.forEach(diff -> columnPositions.add(diff.getColId()));
    }

    for (int i = 1; i <= selectStream.getDataPath().getOrCreateRelationDef().getColumnsSize(); i++) {
      Object object = selectStream.getObject(i);

      if (columnPositions.contains(i)) {
        if (object != null) {
          object = BOLD_CHARACTERS + object + BOLD_CHARACTERS;
        } else {
          object = BOLD_CHARACTERS + "(null)" + BOLD_CHARACTERS;
        }
      }

      row.add(object);
    }

    resultStream.insert(row);


  }


  /**
   * Compare a whole row
   *
   * @return if they are equals
   */
  private Boolean compareAndAddRowData(SelectStream sourceStream, SelectStream targetStream, InsertStream resultStream) {


    List<CellDiff> rowCellDiffs = new ArrayList<>();
    int sourceColumnSize = sourceStream.getDataPath().getOrCreateRelationDef().getColumnsSize();
    for (int i = 1; i <= sourceColumnSize; i++) {

      long rowId = sourceStream.getRow();
      String cellCoordinates = "Cell(Row,Col)(" + rowId + "," + i + ")";

      Object sourceDataPoint = sourceStream.getObject(i);
      Object targetDataPoint = targetStream.getObject(i);
      if (sourceDataPoint != null) {
        if (targetDataPoint == null) {
          rowCellDiffs.add(CellDiff.create(rowId, i));
        } else {
          if (!sourceDataPoint.equals(targetDataPoint)) {

            CellDiff diff = CellDiff.create(rowId, i);
            rowCellDiffs.add(diff);
            /*
             * Case of float
             * TODO: We could use a more broad system when making a not-strict data type comparison
             *  ie if comparison between integer and float, compare in float
             *  ie if comparison between string and date, compare in date
             *  ...
             */
            if (sourceDataPoint.getClass().equals(Double.class)) {
              boolean equalWithFloat = sourceDataPoint.equals(targetDataPoint);
              if (equalWithFloat) {
                diff.setType(Strictness.LOSS_EQUALITY);
              }
            }

            LOGGER.fine(cellCoordinates + ", Diff Found !: " + sourceDataPoint + "," + targetDataPoint);

          } else {

            LOGGER.fine(cellCoordinates + ", No Diff: " + sourceDataPoint + "," + targetDataPoint);

          }
        }
      } else {
        if (targetDataPoint != null) {
          rowCellDiffs.add(CellDiff.create(rowId, i));
          LOGGER.fine(cellCoordinates + ", Diff: (null)," + targetDataPoint);
        } else {
          LOGGER.fine(cellCoordinates + ", No Diff: (null),(null)");
        }
      }

    }

    List<CellDiff> strictCellDiffs = rowCellDiffs.stream()
      .filter(c -> c.getType() != Strictness.LOSS_EQUALITY)
      .collect(Collectors.toList());
    if (strictCellDiffs.size() > 0) {
      areLinesEquals(false);
      addRowData(resultStream, sourceStream, SOURCE_DATA_PATH, rowCellDiffs);
      addRowData(resultStream, targetStream, TARGET_DATA_PATH, rowCellDiffs);
    } else {
      areLinesEquals(true);
      addRowData(resultStream, sourceStream, BOTH_DATA_PATH, rowCellDiffs);
    }

    return rowCellDiffs.size() == 0;

  }

  public DataPath getResultDataPath() {
    if (resultDataPath == null && !hasComparisonRun) {
      throw new RuntimeException("You should run the comparison first with the `compareData` function if you want to get the runtime data path result");
    }
    return resultDataPath;
  }

  /**
   * @return The numbers of row compared
   */
  public Long getRecordCount() {

    if (lineSourceId > lineTargetId) {
      return lineSourceId;
    } else {
      return lineTargetId;
    }

  }

  public boolean areEquals() {
    if (!hasComparisonRun) {
      compareData();
    }
    return numberOfDiffRows == 0;
  }


  /**
   * A cell diff
   */
  private static class CellDiff {

    private final Long rowId;
    private final Integer colId;
    private Strictness type = Strictness.STRICT_EQUALITY;

    /**
     * @param colId - the x position of the cell
     */
    public CellDiff(Long rowId, int colId) {
      this.colId = colId;
      this.rowId = rowId;
    }

    public static CellDiff create(long rowId, int colId) {
      return new CellDiff(rowId, colId);
    }

    void setType(@SuppressWarnings("SameParameterValue") Strictness type) {
      this.type = type;
    }

    public Strictness getType() {
      return type;
    }

    public Integer getColId() {
      return colId;
    }

    public Long getRowId() {
      return rowId;
    }
  }

}
