

package com.tabulify.diff;


import com.tabulify.Tabular;
import com.tabulify.memory.list.MemoryListDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.model.RelationDef;
import com.tabulify.model.UniqueKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.CastException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All function to perform a data path comparison
 * on data level
 * <p>
 * This is set apart of the {@link DataPathComparison}
 * because we make also a comparison on the data of the structure (ie columns)
 */
public class DataPathDiff {


  public static final String DIFF_COLUMN_PREFIX = "diff_";
  private final DataPathDiffBuilder builder;


  public DataPathDiff(DataPathDiffBuilder diffBuilder) {
    this.builder = diffBuilder;
  }

  public static DataPathDiffBuilder builder(Tabular tabular) {
    return new DataPathDiffBuilder(tabular);
  }


  public Tabular getTabular() {
    return this.builder.tabular;
  }


  public DataPathDiffBuilder getBuilder() {
    return this.builder;
  }

  public DataPathDiffResult diff(List<List<?>> source, DataPath target) {


    MemoryListDataPath memoryDataPath = (MemoryListDataPath) getTabular().getAndCreateRandomMemoryDataPath();
    RelationDef relation = memoryDataPath
      .createEmptyRelationDef();
    List<?> data = source.get(0);
    for (int i = 0; i < data.size(); i++) {
      Object o = data.get(i);
      if (o == null) {
        relation.addColumn(String.valueOf(i + 1), String.class);
      } else {
        relation.addColumn(String.valueOf(i + 1), o.getClass());
      }
    }
    memoryDataPath.setValues(source);
    return diff(memoryDataPath, target);

  }

  /**
   * Compare the data
   * There is no columns comparisons to avoid reflective comparison.
   * The passed source and target should have the same number of columns, if not, an error is thrown
   *
   * @param source - the source (ie from, original)
   * @param target - the target (to, new), may be null to define an empty set
   * @return the comparison
   */

  public DataPathDiffResult diff(DataPath source, DataPath target) {

    /**
     * Structure columns check
     */
    int sourceColumnSize = source.getOrCreateRelationDef().getColumnsSize();
    if (target == null) {
      // empty records
      target = getTabular().getAndCreateRandomMemoryDataPath()
        .mergeDataDefinitionFrom(source);
    }
    int targetColumnSize = target.getOrCreateRelationDef().getColumnsSize();
    if (sourceColumnSize != targetColumnSize) {
      throw new IllegalArgumentException("We can't compare the data of the content resource because the source resource (" + source + ") has (" + sourceColumnSize + ") columns while the target resource (" + target + ") has (" + targetColumnSize + ") columns.");
    }
    if (sourceColumnSize == 0) {
      throw new IllegalArgumentException("We can't compare the data of the content resource because the source resource (" + source + ") has no columns.");
    }

    DataPathDiffResult dataPathDiffResult = new DataPathDiffResult(this, source, target);

    // The driver columns column
    List<Integer> driverColumnsPosition = this.getDriverColumnsPosition(source);
    dataPathDiffResult.setDriverColumnPosition(driverColumnsPosition);

    /**
     * Stream cursor management is performed here
     * ie we advance the cursor for the source and target
     * The diff is performed by {@link DataPathDiffResult#streamDiff(SelectStream, SelectStream)}
     */
    try (
      SelectStream sourceStream = source.getSelectStreamSafe();
      SelectStream targetStream = target.getSelectStreamSafe()
    ) {


      // Should we fetch the next record of the source or the target
      boolean sourceNext = true;
      boolean targetNext = true;

      // Does the source and target set has still row
      boolean moreSourceRecord = false;
      boolean moreTargetRecord = false;


      // Loop until no rows anymore in the source and target
      // or max change count
      while (true) {

        if (dataPathDiffResult.getChangeCounter() > this.builder.maxChangeCount) {
          /**
           * There is no way to continue without throwing
           * otherwise the user would see a report with maxChangeCount change
           */
          throw new RuntimeException("The number of changes detected is greater than the maximum allowed (" + this.builder.maxChangeCount + ")");
        }

        /**
         * Source and Target Next Handling
         */
        if (sourceNext) {
          moreSourceRecord = sourceStream.next();
          if (moreSourceRecord) {
            dataPathDiffResult.incrementSourceRecordId();
          } else {
            sourceStream.close();
          }
        }
        if (targetNext) {
          moreTargetRecord = targetStream.next();
          if (moreTargetRecord) {
            dataPathDiffResult.incrementTargetRecordId();
          } else {
            targetStream.close();
          }
        }

        /**
         * Move
         */
        if (!moreSourceRecord && !moreTargetRecord) {
          // No more rows to process
          break;
        }

        DataDiffCells dataDiffCells = new DataDiffCells(builder, sourceStream, targetStream, driverColumnsPosition);

        /**
         * Diff between 2 records
         */
        if (moreSourceRecord && moreTargetRecord) {


          /**
           * Equals or loss otherwise the record is seen as missing
           */
          if (dataDiffCells.isEqualsOrLoss()) {

            dataPathDiffResult.streamDiff(null, dataDiffCells);
            targetNext = true;
            sourceNext = true;
            continue;

          }

          /**
           * The source is greater than the target
           */
          if (dataDiffCells.compare() > 0) {

            dataPathDiffResult.streamDiff(DataPathDiffStatus.ADD, dataDiffCells);
            targetNext = true;
            sourceNext = false;
            continue;

          }

          /**
           * The source is less than the target
           */
          dataPathDiffResult.streamDiff(DataPathDiffStatus.DELETE, dataDiffCells);
          targetNext = false;
          sourceNext = true;
          continue;

        }

        // There is still a source record or a target record
        if (moreSourceRecord) {

          dataPathDiffResult.streamDiff(DataPathDiffStatus.DELETE, dataDiffCells);
          targetNext = false;
          sourceNext = true;
          continue;

        }

        // A target record
        dataPathDiffResult.streamDiff(DataPathDiffStatus.ADD, dataDiffCells);
        targetNext = true;
        sourceNext = false;

      }
    }
    return dataPathDiffResult;
  }

  /**
   * @param source - the source because the target may be empty
   */
  private List<Integer> getDriverColumnsPosition(DataPath source) {

    if (builder.driverColumnNames != null && !builder.driverColumnNames.isEmpty()) {
      List<Integer> driverColumnsPosition = new ArrayList<>();
      for (KeyNormalizer columnName : this.builder.driverColumnNames) {
        ColumnDef columnDef = source.getOrCreateRelationDef().getColumnDef(columnName);
        if (columnDef == null) {
          String knownColumns = source.getOrCreateRelationDef().getColumnDefs()
            .stream()
            .map(ColumnDef::getColumnName)
            .collect(Collectors.joining(", "));
          throw new IllegalArgumentException("The column (" + columnName + ") was not found in the resource (" + source + ") and cannot be used as a driver column (unique column) for the diff. We were expecting one of: " + knownColumns);
        }
        driverColumnsPosition.add(columnDef.getColumnPosition());
      }
      return driverColumnsPosition;
    }


    PrimaryKeyDef targetPrimaryKey = source.getRelationDef().getPrimaryKey();
    if (targetPrimaryKey != null) {
      return targetPrimaryKey.getColumns().stream()
        .map(ColumnDef::getColumnPosition)
        .collect(Collectors.toList());
    }

    List<UniqueKeyDef> uniqueKeys = source.getRelationDef().getUniqueKeys();
    if (uniqueKeys != null && !uniqueKeys.isEmpty()) {
      return uniqueKeys.get(0).getColumns().stream()
        .map(ColumnDef::getColumnPosition)
        .collect(Collectors.toList());
    }

    return source.getRelationDef().getColumnDefs()
      .stream()
      .sorted()
      .map(ColumnDef::getColumnPosition)
      .collect(Collectors.toList());


  }



  public static class DataPathDiffBuilder {


    private final Tabular tabular;

    /**
     * By default, strict because we have now visual clue
     */
    public DataDiffEqualityType equalityType = DataDiffEqualityType.STRICT;
    /**
     * The prefix to column added by the diff
     */
    public String diffColumnPrefix = DIFF_COLUMN_PREFIX;
    /**
     * Use the color or not
     */
    private Boolean useColor;
    /**
     * By default, dense because this is the most common case in the documentation
     * The report density (all or only diff)
     */
    DataDiffReportDensity reportDensity = DataDiffReportDensity.DENSE;


    public DataPathDiffBuilder setReportDiffColumns(List<DataDiffColumn> diffColumns) {
      this.diffColumns = diffColumns;
      return this;
    }

    /**
     * The diff columns
     * They may be set to the empty list to not add them
     * Why? Because on test we don't want to test on them only on the records
     */
    public List<DataDiffColumn> diffColumns = null;

    /**
     * Set a prefix to the diff columns to avoid conflict
     * with the original data
     */
    public DataPathDiffBuilder setReportDiffColumnPrefix(String diffColumnPrefix) {
      this.diffColumnPrefix = diffColumnPrefix;
      return this;
    }

    /**
     * The name of the key column
     */
    List<KeyNormalizer> driverColumnNames;
    private Long maxChangeCount = Long.MAX_VALUE;
    private DataDiffReportAccumulator reportGrain = DataDiffReportAccumulator.UNIFIED;
    /**
     * The location of the result (memory)
     */
    private DataPath resultDataPath;

    public DataPathDiffBuilder(Tabular tabular) {
      this.tabular = tabular;
    }

    public DataPathDiffBuilder setMaxChange(Long maxChangeCount) {
      this.maxChangeCount = maxChangeCount;
      return this;
    }

    /**
     * @param columnNames - the column where the diff occurs (by default, this is the row id). It must be a column where the data is ordered ascendant.
     * @return this data set diff for chaining initialization
     */
    public DataPathDiffBuilder setDriverColumns(String... columnNames) {
      this.driverColumnNames = new ArrayList<>();
      for (String columnName : columnNames) {
        try {
          this.driverColumnNames.add(KeyNormalizer.create(columnName));
        } catch (CastException e) {
          throw new IllegalArgumentException("The driver column name (" + columnName + ") is not a valid name. Error: " + e.getMessage(), e);
        }
      }
      return this;
    }

    public DataPathDiffBuilder setEqualityType(DataDiffEqualityType equalityType) {
      this.equalityType = equalityType;
      return this;
    }

    public DataPathDiffBuilder setReportAccumulator(DataDiffReportAccumulator reportAccumulator) {
      this.reportGrain = reportAccumulator;
      return this;
    }

    public DataPathDiffBuilder setResultDataPath(DataPath resultDataPath) {
      this.resultDataPath = resultDataPath;
      return this;
    }

    public DataPathDiffBuilder setUseTerminalColors(Boolean color) {
      this.useColor = color;
      return this;
    }

    public DataPathDiff build() {

      if (this.diffColumns == null) {
        // status is the default
        this.diffColumns = new ArrayList<>();
        this.diffColumns.add(DataDiffColumn.STATUS);
      }
      /**
       * Add colors by default
       */
      if (this.useColor == null) {
        this.useColor = JavaEnvs.isRunningInTerminal();
      }
      if (this.useColor && !this.diffColumns.contains(DataDiffColumn.COLORS)) {
        this.diffColumns = new ArrayList<>(this.diffColumns);
        this.diffColumns.add(DataDiffColumn.COLORS);
      }
      return new DataPathDiff(this);
    }


    public DataDiffReportAccumulator getReportAccumulatorType() {
      return this.reportGrain;
    }

    public DataPath getResultDataPath() {
      return resultDataPath;
    }

    public DataDiffEqualityType getEqualityType() {
      return this.equalityType;
    }

    public List<KeyNormalizer> getDriverColumnsNames() {
      return this.driverColumnNames;
    }

    public boolean getUseColor() {
      return this.useColor;
    }

    public DataPathDiffBuilder setReportDensity(DataDiffReportDensity dataDiffReportDensity) {
      this.reportDensity = dataDiffReportDensity;
      return this;
    }
  }
}
