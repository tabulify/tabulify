package com.tabulify.diff;

import com.tabulify.memory.MemoryConnection;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;

import java.util.List;


public class DataPathDiffResult {


  private final DataPathDiff dataPathDiff;
  private final DataPath source;
  private final DataPath target;
  final DataPathDiff.DataPathDiffBuilder diffBuilderMeta;
  /**
   * May be null if the report is only the summary
   */
  private final DataPathDiffReport accumulatorReport;
  /**
   * The driver column positions used
   */
  private List<Integer> driverColumnPositionList;
  /**
   * Line deletion counter
   */
  private Long deletionCounter = 0L;


  public DataPathDiffResult(DataPathDiff dataPathDiff, DataPath source, DataPath target) {

    this.dataPathDiff = dataPathDiff;
    this.source = source;
    this.target = target;
    this.diffBuilderMeta = dataPathDiff.getBuilder();
    this.accumulatorReport = buildReport();


  }

  /**
   * The data path that will accumulate the changes
   */
  private DataPathDiffReport buildReport() {
    DataPath resultDataPath = this.diffBuilderMeta.getResultDataPath();
    if (resultDataPath == null) {
      resultDataPath = dataPathDiff.getTabular()
        .getMemoryConnection()
        .getAndCreateRandomDataPath();
    }
    /**
     * The report should not exist for now
     */
    if (!(resultDataPath.getConnection() instanceof MemoryConnection)) {
      if (Tabulars.exists(resultDataPath)) {
        throw new IllegalArgumentException("The diff result resource (" + resultDataPath + ") should not exists");
      }
    }


    /**
     * The type of report to accumulate
     */
    DataDiffReportAccumulator reportType = this.diffBuilderMeta.getReportAccumulatorType();
    DataPathDiffReport dataPathDiffReport;
    switch (reportType) {
      case UNIFIED:
        dataPathDiffReport = new DataPathDiffUnifiedRecordReport(this, resultDataPath);
        break;
      case CELL:
        dataPathDiffReport = new DataPathDiffCellReport(this, resultDataPath);
        break;
      case NONE:
        return null;
      default:
        throw new UnsupportedOperationException("We don't support the report type (" + reportType + ") yet");
    }
    Tabulars.createIfNotExist(resultDataPath);
    return dataPathDiffReport;

  }


  /**
   * A total report
   */
  public DataPath getResultSummaryReport() {
    return this.dataPathDiff.getTabular()
      .getMemoryConnection().getAndCreateRandomDataPath()
      .setLogicalName("diff_summary_report")
      .setComment("Diff Summary Report\n" +
        "The data resources are " + (!this.areEquals() ? "not " : "") + "equals.")
      .createRelationDef()
      .addColumn("from", SqlDataTypeAnsi.CHARACTER_VARYING)
      .addColumn("to", SqlDataTypeAnsi.CHARACTER_VARYING)
      .addColumn("equals", SqlDataTypeAnsi.BOOLEAN)
      .addColumn("record_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("change_count", SqlDataTypeAnsi.INTEGER)
      .getDataPath()
      .getInsertStream()
      .insert(source, target, this.areEquals(), this.getRecordCount(), this.getChangeCounter())
      .getDataPath();
  }

  public DataPath getResultAccumulatorReport() {

    return this.accumulatorReport.getDataPath()
      .setComment(accumulatorReport.getDataPath().getComment() +
        "\nThe data resources are " + (!this.areEquals() ? "not " : "") + "equals.");
  }

  public boolean areEquals() {

    return this.getChangeCounter() == 0;
  }

  public Long getChangeCounter() {

    return changeCounter;

  }

  public Long getDeletionCounter() {

    return deletionCounter;

  }


  public DataPath getSource() {
    return source;
  }

  /**
   * @return The numbers of record compared
   */
  public Long getRecordCount() {

    if (sourceRecordId > targetRecordId) {
      return sourceRecordId;
    }
    return targetRecordId;

  }

  /**
   * The number of record in the accumulator report
   */
  @SuppressWarnings("unused")
  public Long getRecordReportCounter() {

    return this.recordDiffCounter;

  }


  /**
   * Utility to:
   * * get the diff between the source and the target
   * * to insert the result
   * *
   * There is only 4 cases:
   * <p>
   * A record is present only in the first data path, one insert of
   * * selectStream is the first stream
   * * type {@link DataPathDiffStatus#DELETE}
   * <p>
   * A record is present only in the second data path, one insert of
   * * selectStream is the second stream
   * * type {@link DataPathDiffStatus#ADD}
   * <p>
   * A record is the same, one insert of
   * * selectStream is the first or second stream
   * * type {@link DataPathDiffStatus#NO_CHANGE}
   * * no coordinates cell
   * <p>
   * A row is not the same: two inserts
   * <p>
   * First:
   * * first selectStream
   * * type {@link DataPathDiffStatus#VALUE}
   * * coordinates cell of the diff
   * and
   * Second:
   * * second selectStream
   * * type {@link DataPathDiffStatus#VALUE}
   * * coordinates cell of the diff
   */
  void streamDiff(DataPathDiffStatus diffStatus, DataDiffCells dataDiffCells) {


    recordDiffCounter++;


    if (diffStatus == DataPathDiffStatus.ADD) {
      incrementChangeCounter();
      if (this.accumulatorReport != null) {
        this.accumulatorReport.insertResultRecord(dataDiffCells, DataPathDiffStatus.ADD);
      }
      return;
    }

    if (diffStatus == DataPathDiffStatus.DELETE) {
      incrementChangeCounter();
      incrementDeletionCounter();
      if (this.accumulatorReport != null) {
        this.accumulatorReport.insertResultRecord(dataDiffCells, DataPathDiffStatus.DELETE);
      }
      return;
    }

    boolean changeDetected = !dataDiffCells.isEquals();
    if (changeDetected) {
      incrementChangeCounter();
      incrementDeletionCounter();
      if (this.accumulatorReport != null) {
        this.accumulatorReport.insertResultRecord(dataDiffCells, DataPathDiffStatus.VALUE);
      }
      return;
    }

    /**
     * No Change
     */
    if (this.accumulatorReport != null) {
      this.accumulatorReport.insertResultRecord(dataDiffCells, DataPathDiffStatus.NO_CHANGE);
    }


  }

  /**
   * The number of record seen
   */
  private long recordDiffCounter = 0;

  /**
   * The number of record with change
   */
  private long changeCounter = 0;

  private void incrementChangeCounter() {

    changeCounter++;

  }

  private void incrementDeletionCounter() {

    deletionCounter++;

  }


  public DataPath getTarget() {
    return this.target;
  }

  /**
   * Record counter
   */
  private Long sourceRecordId = 0L;
  private Long targetRecordId = 0L;


  /**
   * The number of source record
   */
  public void incrementSourceRecordId() {
    sourceRecordId++;
  }

  /**
   * The number of target record
   */
  public void incrementTargetRecordId() {
    targetRecordId++;
  }

  public Long getSourceRecordId() {
    return this.sourceRecordId;
  }

  public Long getTargetRecordId() {
    return this.targetRecordId;
  }


  public DataPathDiffResult setDriverColumnPosition(List<Integer> driverColumnsPosition) {
    this.driverColumnPositionList = driverColumnsPosition;
    return this;
  }

  public List<Integer> getDriverColumnPositions() {
    return this.driverColumnPositionList;
  }

  public DataPathDiff.DataPathDiffBuilder getBuilder() {
    return diffBuilderMeta;
  }

  /**
   * @return true if the source is contained in the target
   * (ie no deletion needed)
   */
  public boolean isSourceContainedInTarget() {
    return this.deletionCounter == 0;
  }

}

