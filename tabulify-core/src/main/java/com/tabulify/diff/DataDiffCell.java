package com.tabulify.diff;

import com.tabulify.model.ColumnDef;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;

/**
 * A diff cell is a source value and a target value
 * with optional coordinates
 * that encapsulate the equality and compare function
 */
public class DataDiffCell {


  private final CellDiffBuilder builder;
  private final DataDiffEqualityStatus equalityStatus;
  private final DataPathDiff.DataPathDiffBuilder meta;


  public DataDiffCell(CellDiffBuilder cellDiffBuilder) {
    this.builder = cellDiffBuilder;
    this.meta = cellDiffBuilder.meta;
    equalityStatus = computeLossEquality();

  }


  public static CellDiffBuilder builder(DataDiffCells dataDiffCells, Object sourceValue, Object targetValue) {
    return new CellDiffBuilder(dataDiffCells, sourceValue, targetValue);
  }


  public DataDiffEqualityStatus getEqualityStatus() {
    return equalityStatus;
  }


  public Long getRecordId() {
    return builder.recordId;
  }

  private DataDiffEqualityStatus computeLossEquality() {

    /**
     * Null case
     */
    if (builder.sourceValue == null) {
      if (builder.targetValue != null) {
        if (builder.targetValue instanceof String) {
          if (((String) builder.targetValue).isBlank()) {
            return DataDiffEqualityStatus.LOSS_EQUAL;
          }
        }
        return DataDiffEqualityStatus.NOT_EQUAL;
      }
      return DataDiffEqualityStatus.STRICT_EQUAL;
    }

    if (builder.targetValue == null) {
      if (builder.sourceValue instanceof String) {
        if (((String) builder.sourceValue).isBlank()) {
          return DataDiffEqualityStatus.LOSS_EQUAL;
        }
      }
      return DataDiffEqualityStatus.NOT_EQUAL;
    }

    if (builder.sourceValue instanceof String && builder.targetValue instanceof String) {
      if (((String) builder.sourceValue).isBlank() && ((String) builder.targetValue).isBlank()) {
        return DataDiffEqualityStatus.LOSS_EQUAL;
      }
    }
    boolean strictEqual = builder.sourceValue.equals(builder.targetValue);
    if (strictEqual) {
      return DataDiffEqualityStatus.STRICT_EQUAL;
    }


    /**
     * We still calculate the loss equality
     * because this is extra util information for the user
     * <p>
     * Case of float, time, ...
     * Can we cast the source to the target
     */
    if (
      builder.sourceValue.getClass() == builder.targetValue.getClass()
    ) {
      return DataDiffEqualityStatus.NOT_EQUAL;
    }

    try {
      Object sourceValueAsTarget = Casts.cast(builder.sourceValue, builder.targetValue.getClass());
      if (sourceValueAsTarget == null) {
        // casting an empty string as integer for instance is null
        return DataDiffEqualityStatus.NOT_EQUAL;
      }
      if (sourceValueAsTarget.equals(builder.targetValue)) {
        return DataDiffEqualityStatus.LOSS_EQUAL;
      }
    } catch (CastException e) {
      // can not be cast, definitely not equals
    }

    return DataDiffEqualityStatus.NOT_EQUAL;


  }

  public boolean isEquals() {
    DataDiffEqualityType equalityType = this.builder.meta.getEqualityType();
    switch (equalityType) {
      case STRICT:
        return equalityStatus == DataDiffEqualityStatus.STRICT_EQUAL;
      case LOSS:
        return equalityStatus == DataDiffEqualityStatus.STRICT_EQUAL || equalityStatus == DataDiffEqualityStatus.LOSS_EQUAL;
      default:
        throw new InternalError("The equality type (" + equalityType + ") should have been implemented");
    }

  }

  public int compare() {
    /**
     * Null handling
     */
    if (builder.sourceValue == null) {
      if (builder.targetValue != null) {
        return -1;
      }
      return 0;
    }
    if (builder.targetValue == null) {
      return 1;
    }
    /**
     * Value base comparison
     */
    if (
      builder.sourceValue.getClass() == builder.targetValue.getClass()
    ) {
      if (builder.targetValue instanceof Comparable) {
        //noinspection unchecked
        return ((Comparable<Object>) this.builder.sourceValue).compareTo(this.builder.targetValue);
      }
    }
    if (this.meta.getEqualityType() == DataDiffEqualityType.LOSS) {
      try {
        Object sourceValueAsTarget = Casts.cast(builder.sourceValue, builder.targetValue.getClass());
        //noinspection unchecked
        return ((Comparable<Object>) sourceValueAsTarget).compareTo(this.builder.targetValue);
      } catch (CastException e) {
        // ok, target value can not be cast
      }
    }
    return this.builder.sourceValue.toString().compareTo(this.builder.targetValue.toString());
  }

  public Object getSourceValue() {
    return this.builder.sourceValue;
  }

  public Object getTargetValue() {
    return this.builder.targetValue;
  }

  public ColumnDef getColumnDef() {
    return this.builder.columnDef;
  }

  public boolean isDriverCell() {
    return this.builder.dataDiffCells.getDriverColumnPositions().contains(this.builder.columnDef.getColumnPosition());
  }

  public static class CellDiffBuilder {
    private final DataPathDiff.DataPathDiffBuilder meta;
    private final DataDiffCells dataDiffCells;
    private long recordId;
    private ColumnDef<?> columnDef;
    private final Object sourceValue;
    private final Object targetValue;


    public CellDiffBuilder(DataDiffCells dataDiffCells, Object sourceValue, Object targetValue) {
      this.dataDiffCells = dataDiffCells;
      this.meta = dataDiffCells.getDataDiffBuilder();
      this.sourceValue = sourceValue;
      this.targetValue = targetValue;
    }


    public CellDiffBuilder setRecordId(long recordId) {
      this.recordId = recordId;
      return this;
    }


    public CellDiffBuilder setColumnDef(ColumnDef columnDef) {
      this.columnDef = columnDef;
      return this;
    }

    public DataDiffCell build() {

      return new DataDiffCell(this);
    }

  }
}
