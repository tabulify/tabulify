package com.tabulify.flow.operation;

import com.tabulify.diff.DataDiffColumn;
import com.tabulify.diff.DataDiffEqualityType;
import com.tabulify.diff.DataDiffReportDensity;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepBuilderTargetArgument;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The diff extends {@link PipelineStepBuilderTarget} and get therefore {@link PipelineStepBuilderTargetArgument}
 */
public enum DiffPipelineStepArgument implements ArgumentEnum {

  DATA_ORIGIN("The data origin of the diff (record, structure or attributes)", DiffPipelineStepDataOrigin.class, DiffPipelineStepDataOrigin.RECORD),
  // By default, a summary
  // It's the most compact report and the most used in the documentation
  // to perform a test
  REPORT_TYPE("The type of report", DiffPipelineStepReportType.class, DiffPipelineStepReportType.SUMMARY),
  // Driver columns
  // By default, this is
  //  * the record id (ie row id, line id) for a data comparison,
  //  * the column name for a data structure comparison
  DRIVER_COLUMNS("The column names that drive the diff (unique columns normally)", List.class, new ArrayList<>()),
  MAX_CHANGE_COUNT("The maximum number of changes detected before stopping the diff", Long.class, 100L),
  EQUALITY_TYPE("The type of equality (loss, strict)", DataDiffEqualityType.class, DataDiffEqualityType.LOSS),
  FAIL("When true, a diff with inequality will fail", Boolean.class, true),
  REPORT_DATA_URI("Defines where the accumulator report should be stored", DataUriStringNode.class, Constants.DEFAULT_DIFF_DATA_URI),
  REPORT_DIFF_COLUMNS("List of diff columns added to the unified report", List.class, List.of(DataDiffColumn.STATUS)),
  REPORT_DIFF_COLUMN_PREFIX("A prefix added the diff columns", String.class, "diff_"),
  REPORT_DENSITY("if sparse, the equals data are not shown (known as context in text diff report)", Boolean.class, DataDiffReportDensity.DENSE),
  TERMINAL_COLORS("if true, the output is colored", Boolean.class, null);

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  DiffPipelineStepArgument(String description, Class<?> aClass, Object defaultValue) {
    this.description = description;
    this.clazz = aClass;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

  private static class Constants {
    public static final DataUriStringNode DEFAULT_DIFF_DATA_URI = DataUriStringNode.createFromStringSafe("diff_${input_logical_name}_${target_logical_name}@memory");


  }
}
