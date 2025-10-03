package com.tabulify.diff;

import com.tabulify.model.ColumnDef;
import com.tabulify.stream.SelectStream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class that wraps a record to drive the comparison
 */
public class DataDiffCells {

  private final List<Integer> driverColumnPosition;
  private final DataPathDiff.DataPathDiffBuilder diffMeta;
  private final SelectStream driverStream;
  private final SelectStream sourceStream;
  private final SelectStream targetStream;
  private final List<DataDiffCell> cells;

  /**
   * All parameters are mandatory
   *
   * @param builder               - the diff builder
   * @param sourceStream          the source
   * @param targetStream          the target
   * @param driverColumnsPosition - the driver columns position (all or a subset)
   */
  public DataDiffCells(DataPathDiff.DataPathDiffBuilder builder, SelectStream sourceStream, SelectStream targetStream, List<Integer> driverColumnsPosition) {



    if (sourceStream == null && targetStream == null) {
      throw new InternalError("The source stream and target stream are both null");
    }
    if (driverColumnsPosition == null || driverColumnsPosition.isEmpty()) {
      // We compare on the whole set of columns or a subset, but we need some
      throw new InternalError("The driver columns position should not be null");
    }

    this.sourceStream = sourceStream;
    this.targetStream = targetStream;
    SelectStream tempDriverStream = sourceStream;
    if (tempDriverStream == null) {
      tempDriverStream = targetStream;
    }
    this.driverStream = tempDriverStream;
    this.driverColumnPosition = driverColumnsPosition;
    this.diffMeta = builder;
    this.cells = getRecordCellDiffs();
  }


  /**
   * Return all cell
   *
   * @return the diff
   */
  private List<DataDiffCell> getRecordCellDiffs() {


    List<DataDiffCell> cellDiffs = new ArrayList<>();

    for (ColumnDef sourceColumnDef : driverStream.getRuntimeRelationDef().getColumnDefs()) {

      Object sourceValue = null;
      if (sourceStream != null && !sourceStream.isClosed()) {
        sourceValue = sourceStream.getObject(sourceColumnDef.getColumnPosition());
      }
      Object targetValue = null;
      if (targetStream != null && !targetStream.isClosed()) {
        targetValue = targetStream.getObject(sourceColumnDef.getColumnPosition());
      }
      cellDiffs.add(
        DataDiffCell.builder(this, sourceValue, targetValue)
          .setRecordId(driverStream.getRecordId())
          .setColumnDef(sourceColumnDef)
          .build()
      );

    }

    return cellDiffs;

  }


  int compare() {
    for (DataDiffCell dataDiffCell : this.cells) {
      if (!dataDiffCell.isDriverCell()) {
        continue;
      }
      int compare = dataDiffCell.compare();
      if (compare != 0) {
        return compare;
      }
    }
    return 0;
  }

  public boolean isEquals() {
    List<DataDiffCell> notEqualsCells = cells.stream()
      .filter(c -> {
        if (this.diffMeta.equalityType == DataDiffEqualityType.STRICT) {
            /**
             * All not strict equals are considered as not equal
             */
            return c.getEqualityStatus() != DataDiffEqualityStatus.STRICT_EQUAL;
          }
          return c.getEqualityStatus() == DataDiffEqualityStatus.NOT_EQUAL;
        }
      )
      .collect(Collectors.toList());
    return notEqualsCells.isEmpty();
  }

  public boolean isEmpty() {
    return this.driverColumnPosition.isEmpty();
  }

  public List<DataDiffCell> getCells() {
    return this.cells;
  }

  /**
   * @return if the equality is loss
   */
  public boolean isEqualsOrLoss() {
    List<DataDiffCell> notEqualsCells = cells.stream()
      .filter(DataDiffCell::isDriverCell)
      .filter(c -> c.getEqualityStatus() == DataDiffEqualityStatus.NOT_EQUAL
      )
      .collect(Collectors.toList());
    return notEqualsCells.isEmpty();
  }

  public DataPathDiff.DataPathDiffBuilder getDataDiffBuilder() {
    return this.diffMeta;
  }

  public List<Integer> getDriverColumnPositions() {
    return this.driverColumnPosition;
  }

  public SelectStream getSourceStream() {
    return this.sourceStream;
  }

  public SelectStream getTargetStream() {
    return this.targetStream;
  }
}
