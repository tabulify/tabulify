package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.diff.*;
import com.tabulify.flow.FlowLog;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.model.ColumnAttribute;
import com.tabulify.spi.AttributeProperties;
import com.tabulify.spi.DataPath;
import com.tabulify.template.TemplateMetas;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

public class DiffPipelineStep extends PipelineStepIntermediateMapAbs {


  private final DiffPipelineStepBuilder diffBuilder;

  public DiffPipelineStep(DiffPipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.diffBuilder = pipelineStepBuilder;
  }

  static public DiffPipelineStepBuilder builder() {
    return new DiffPipelineStepBuilder();
  }


  @Override
  public DataPath apply(DataPath input) {


    /**
     * Report Diff Data Uri
     * Add the input and target variable map
     */
    DataPath target = diffBuilder.getTargetFromInput(input);

    DataPath reportDiffDataPath = diffBuilder.reportDiffDataUriFunction.apply(input, TemplateMetas
      .builder()
      .addInputDataPath(input)
      .addTargetDataPath(target)
    );


    /**
     * Start building the diff
     */
    DataPathDiff.DataPathDiffBuilder diffBuilder = DataPathDiff
      .builder(getTabular())
      .setMaxChange(this.diffBuilder.maxChangeCount)
      .setEqualityType(this.diffBuilder.equalityType)
      .setReportDiffColumnPrefix(this.diffBuilder.diffColumnPrefix)
      .setReportDiffColumns(this.diffBuilder.dataDiffColumns)
      .setUseTerminalColors(this.diffBuilder.useTerminalColors)
      .setReportDensity(this.diffBuilder.reportDensity)
      .setResultDataPath(reportDiffDataPath);
    switch (this.diffBuilder.reportType) {
      case UNIFIED:
        diffBuilder.setReportAccumulator(DataDiffReportAccumulator.UNIFIED);
        break;
      case CELL:
        diffBuilder.setReportAccumulator(DataDiffReportAccumulator.CELL);
        break;
      case SUMMARY:
        diffBuilder.setReportAccumulator(DataDiffReportAccumulator.NONE);
        break;
      default:
        throw new InternalException("The report type (" + this.diffBuilder.reportType + ") was not implemented");
    }

    DataPathDiffResult diffResult;
    switch (this.diffBuilder.dataOrigin) {

      case RECORD:

        FlowLog.LOGGER.info("Data Diff started between the input (" + input + ") and the target (" + target + ")");
        if (!this.diffBuilder.driverColumns.isEmpty()) {
          diffBuilder.setDriverColumns(this.diffBuilder.driverColumns.toArray(new String[0]));
        }
        diffResult = diffBuilder.build().diff(input, target);
        break;

      case STRUCTURE:
        FlowLog.LOGGER.info("Structure Diff Comparison started between the input (" + input + ") and the target (" + target + ")");
        ColumnAttribute driverAttribute = ColumnAttribute.NAME;
        if (!this.diffBuilder.driverColumns.isEmpty()) {
          String inputObject = this.diffBuilder.driverColumns.get(0);
          try {
            driverAttribute = Casts.cast(inputObject, ColumnAttribute.class);
          } catch (CastException e) {
            throw new RuntimeException("The driver value (" + inputObject + ") is not a column attribute for the step (" + this + ")", e);
          }
        }
        diffResult = diffBuilder
          .setDriverColumns(KeyNormalizer.createSafe(driverAttribute).toSqlCase())
          .build().diff(input
              .getOrCreateRelationDef()
              .toColumnsDataPathBy(driverAttribute),
            target
              .getOrCreateRelationDef()
              .toColumnsDataPathBy(driverAttribute));
        break;
      case ATTRIBUTES:
        FlowLog.LOGGER.info("Attributes Diff Comparison started between the input (" + input + ") and the target (" + target + ")");
        diffResult = diffBuilder
          .setDriverColumns(AttributeProperties.ATTRIBUTE.toString())
          .build().diff(input.toAttributesDataPath(), target.toAttributesDataPath());
        break;
      default:
        throw new IllegalArgumentException("The input diff operation (" + this.diffBuilder.dataOrigin + ") should get a processing function associated");
    }


    if (!diffResult.areEquals() && this.diffBuilder.fail) {
      getPipeline().setExitStatus(1);
    }

    switch (this.diffBuilder.reportType) {
      case SUMMARY:
        return diffResult.getResultSummaryReport();
      default:
        return diffResult.getResultAccumulatorReport();
    }


  }


  public static class DiffPipelineStepBuilder extends PipelineStepBuilderTarget {

    static final KeyNormalizer DIFF = KeyNormalizer.createSafe("diff");

    private Boolean fail = (Boolean) DiffPipelineStepArgument.FAIL.getDefaultValue();
    private List<String> driverColumns = new ArrayList<>();
    private DiffPipelineStepReportType reportType = (DiffPipelineStepReportType) DiffPipelineStepArgument.REPORT_TYPE.getDefaultValue();
    private DiffPipelineStepDataOrigin dataOrigin = (DiffPipelineStepDataOrigin) DiffPipelineStepArgument.DATA_ORIGIN.getDefaultValue();
    private Long maxChangeCount = (Long) DiffPipelineStepArgument.MAX_CHANGE_COUNT.getDefaultValue();
    private DataDiffEqualityType equalityType = (DataDiffEqualityType) DiffPipelineStepArgument.EQUALITY_TYPE.getDefaultValue();
    private DataUriStringNode reportDataUriString = (DataUriStringNode) DiffPipelineStepArgument.REPORT_DATA_URI.getDefaultValue();
    private TemplateUriFunction reportDiffDataUriFunction;
    @SuppressWarnings("unchecked")
    private List<DataDiffColumn> dataDiffColumns = (List<DataDiffColumn>) DiffPipelineStepArgument.REPORT_DIFF_COLUMNS.getDefaultValue();
    private String diffColumnPrefix = (String) DiffPipelineStepArgument.REPORT_DIFF_COLUMN_PREFIX.getDefaultValue();
    private Boolean useTerminalColors;

    public DiffPipelineStepBuilder setReportDensity(DataDiffReportDensity reportDensity) {
      this.reportDensity = reportDensity;
      return this;
    }

    private DataDiffReportDensity reportDensity = (DataDiffReportDensity) DiffPipelineStepArgument.REPORT_DENSITY.getDefaultValue();

    @Override
    public DiffPipelineStepBuilder createStepBuilder() {
      return new DiffPipelineStepBuilder();
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      List<Class<? extends ArgumentEnum>> list = new ArrayList<>(super.getArgumentEnums());
      list.add(DiffPipelineStepArgument.class);
      return list;
    }

    public DiffPipelineStepBuilder setDriverColumns(List<String> driverColumns) {
      this.driverColumns = driverColumns;
      return this;
    }

    public DiffPipelineStepBuilder setReportType(DiffPipelineStepReportType report) {
      this.reportType = report;
      return this;
    }

    public DiffPipelineStepBuilder setDataOrigin(DiffPipelineStepDataOrigin dataOrigin) {
      this.dataOrigin = dataOrigin;
      return this;
    }

    /**
     * Set if a change results in an error status code
     *
     * @param fail - true or false
     */
    public DiffPipelineStepBuilder setFail(Boolean fail) {
      this.fail = fail;
      return this;
    }

    @Override
    public PipelineStep build() {

      DataUriNode reportDataUri = this.getPipelineBuilder().getDataUri(this.reportDataUriString);
      this.reportDiffDataUriFunction = TemplateUriFunction.builder(getTabular())
        .setPipeline(this.getPipeline())
        .setTargetUri(reportDataUri)
        .setStrict(this.getPipeline().isStrict())
        .build();

      return new DiffPipelineStep(this);
    }

    @Override
    public DiffPipelineStepBuilder setArgument(KeyNormalizer argumentKey, Object value) {

      DiffPipelineStepArgument diffArgument;
      try {
        diffArgument = Casts.cast(argumentKey, DiffPipelineStepArgument.class);
      } catch (CastException e) {
        // may be a target arguments
        super.setArgument(argumentKey, value);
        return this;
      }

      Attribute attribute;
      try {
        attribute = this.getTabular().getVault()
          .createVariableBuilderFromAttribute(diffArgument)
          .setOrigin(Origin.PIPELINE)
          .build(value);
        this.setArgument(attribute);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + diffArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
      }

      switch (diffArgument) {
        case FAIL:
          this.setFail(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
          break;
        case DATA_ORIGIN:
          this.setDataOrigin(attribute.getValueOrDefaultCastAsSafe(DiffPipelineStepDataOrigin.class));
          break;
        case REPORT_TYPE:
          this.setReportType(attribute.getValueOrDefaultCastAsSafe(DiffPipelineStepReportType.class));
          break;
        case DRIVER_COLUMNS:
          try {
            this.setDriverColumns(Casts.castToNewList(attribute.getValueOrNull(), String.class));
          } catch (CastException e) {
            throw new IllegalArgumentException("The " + diffArgument + " value (" + value + ") of the step (" + this + ") is not conform (not a list). Error: " + e.getMessage(), e);
          }
          break;
        case MAX_CHANGE_COUNT:
          this.setMaxChangeCount((Long) attribute.getValueOrDefault());
          break;
        case EQUALITY_TYPE:
          this.setEqualityType((DataDiffEqualityType) attribute.getValueOrDefault());
          break;
        case REPORT_DATA_URI:
          this.setReportDataUriString((DataUriStringNode) attribute.getValueOrDefault());
          break;
        case REPORT_DIFF_COLUMNS:
          @SuppressWarnings("unchecked") List<String> reportDiffColumns = (List<String>) attribute.getValueOrDefault();
          List<DataDiffColumn> dataDiffColumns = new ArrayList<>();
          for (String columnName : reportDiffColumns) {
            try {
              dataDiffColumns.add(Casts.cast(columnName, DataDiffColumn.class));
            } catch (CastException e) {
              throw new IllegalArgumentException("The " + diffArgument + " has a column value (" + columnName + ") that was not expected. We were expecting one of " + Enums.toConstantAsStringCommaSeparated(DataDiffColumn.class), e);
            }
          }
          this.setDiffColumns(dataDiffColumns);
          break;
        case REPORT_DIFF_COLUMN_PREFIX:
          this.setDiffColumnPrefix(attribute.getValueOrDefaultAsStringNotNull());
          break;
        case REPORT_DENSITY:
          this.setReportDensity((DataDiffReportDensity) attribute.getValueOrDefault());
          break;
        case TERMINAL_COLORS:
          this.setTerminalColors((Boolean) attribute.getValueOrDefault());
          break;
        default:
          throw new InternalException("The " + diffArgument + " value (" + value + ") of the step (" + this + ") should have been processed");
      }
      return this;
    }

    public DiffPipelineStepBuilder setTerminalColors(Boolean useColor) {
      this.useTerminalColors = useColor;
      return this;
    }

    public DiffPipelineStepBuilder setDiffColumnPrefix(String prefix) {
      this.diffColumnPrefix = prefix;
      return this;
    }

    public void setDiffColumns(List<DataDiffColumn> dataDiffColumns) {
      this.dataDiffColumns = dataDiffColumns;
    }

    public DiffPipelineStepBuilder setReportDataUriString(DataUriStringNode diffDataUri) {
      this.reportDataUriString = diffDataUri;
      return this;
    }

    public DiffPipelineStepBuilder setEqualityType(DataDiffEqualityType equalityType) {
      this.equalityType = equalityType;
      return this;
    }

    public DiffPipelineStepBuilder setMaxChangeCount(Long maxChangeCount) {
      this.maxChangeCount = maxChangeCount;
      return this;
    }


    @Override
    public KeyNormalizer getOperationName() {
      return DIFF;
    }
  }
}
