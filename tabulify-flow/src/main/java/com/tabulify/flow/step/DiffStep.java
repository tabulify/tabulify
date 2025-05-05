package com.tabulify.flow.step;

import com.tabulify.diff.DataPathDataComparison;
import com.tabulify.flow.FlowLog;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.model.ColumnAttribute;
import com.tabulify.spi.AttributeProperties;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tabulify.flow.step.DiffStepSource.CONTENT;

public class DiffStep extends TargetFilterStepAbs {


  private List<String> driverColumns;
  private DiffStepReportType report = DiffStepReportType.RESOURCE;
  private DiffStepSource source = CONTENT;

  public DiffStep() {

    this.getOrCreateArgument(DiffStepArgument.SOURCE).setValueProvider(() -> this.source);
    this.getOrCreateArgument(DiffStepArgument.REPORT).setValueProvider(() -> this.report);
    this.getOrCreateArgument(DiffStepArgument.DRIVER_COLUMNS).setValueProvider(() -> this.driverColumns);


  }

  public static DiffStep create() {
    return new DiffStep();
  }

  @Override
  public String getOperationName() {
    return "diff";
  }


  @Override
  public FilterRunnable createRunnable() {
    return new CompareRunnable(this);
  }

  public DiffStep setDriverColumns(List<String> driverColumns) {
    this.driverColumns = driverColumns;
    return this;
  }

  public DiffStep setReport(DiffStepReportType report) {
    this.report = report;
    return this;
  }

  public DiffStep setSource(DiffStepSource source) {
    this.source = source;
    return this;
  }


  private class CompareRunnable implements FilterRunnable {

    private final DiffStep compareOperation;
    private final Set<DataPath> inputs = new HashSet<>();
    private final Set<DataPath> feedbackDataPaths = new HashSet<>();
    private boolean isDone = false;

    public CompareRunnable(DiffStep compareOperation) {
      this.compareOperation = compareOperation;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {


      DataPath comparisonReport = tabular.getMemoryDataStore().getAndCreateRandomDataPath()
        .setLogicalName("diff_report")
        .setDescription("Diff Report")
        .createRelationDef()
        .addColumn("Source", Types.VARCHAR)
        .addColumn("Target", Types.VARCHAR)
        .addColumn("Equals", Types.BOOLEAN)
        .addColumn("DiffCount", Types.INTEGER)
        .addColumn("RecordCount", Types.INTEGER)
        .getDataPath();


      try (InsertStream insertStream = comparisonReport.getInsertStream()) {


        getSourceTarget(inputs).forEach((source, target) -> {

          DataPathDataComparison comp;
          switch (this.compareOperation.source) {

            case CONTENT:

              FlowLog.LOGGER.info("Data Diff Comparison started between the source (" + source + ") and the target (" + target + ")");
              comp = DataPathDataComparison.create(source, target);
              if (!driverColumns.isEmpty()) {
                comp.setUniqueColumns(driverColumns.toArray(new String[0]));
              }
              comp.compareData();
              break;

            case STRUCTURE:
              FlowLog.LOGGER.info("Structure Diff Comparison started between the source (" + source + ") and the target (" + target + ")");
              ColumnAttribute driverAttribute = ColumnAttribute.NAME;
              if (!driverColumns.isEmpty()) {
                String sourceObject = driverColumns.get(0);
                try {
                  driverAttribute = Casts.cast(sourceObject, ColumnAttribute.class);
                } catch (CastException e) {
                  throw new RuntimeException("The driver value (" + sourceObject + ") is not a column attribute for the step (" + this + ")", e);
                }
              }
              comp = DataPathDataComparison.create(source
                    .getOrCreateRelationDef()
                    .toColumnsDataPathBy(driverAttribute),
                  target
                    .getOrCreateRelationDef()
                    .toColumnsDataPathBy(driverAttribute))
                .setUniqueColumns(KeyNormalizer.create(driverAttribute).toSqlCase())
                .compareData();
              break;
            case ATTRIBUTE:
              FlowLog.LOGGER.info("Attributes Diff Comparison started between the source (" + source + ") and the target (" + target + ")");
              comp =
                DataPathDataComparison.create(source
                      .toAttributesDataPath(),
                    target
                      .toAttributesDataPath())
                  .setUniqueColumns(AttributeProperties.ATTRIBUTE.toString())
                  .compareData();
              break;
            default:
              throw new IllegalArgumentException("The source compare operation (" + this.compareOperation.source + ") should get a processing function associated");
          }

          if (Arrays.asList(DiffStepReportType.ALL, DiffStepReportType.RECORD).contains(report)) {
            feedbackDataPaths.add(comp.getResultDataPath());
          }

          /*
           * We may test after the execution and
           * when creating documentation, we get also an error
           * In dev, no errors if the source and target are not the same
           * This should be an attribute of the diff
           */
          if (!comp.areEquals() && !tabular.isIdeEnv()) {
            tabular.setExitStatus(1);
          }

          insertStream.insert(source, target, comp.areEquals(), comp.getDiffCount(), comp.getRecordCount());
        });
      }

      if (Arrays.asList(DiffStepReportType.ALL, DiffStepReportType.RESOURCE).contains(report)) {
        feedbackDataPaths.add(comparisonReport);
      }

      isDone = true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return isDone;
    }

    @Override
    public Set<DataPath> get() {
      return feedbackDataPaths;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) {
      return get();
    }
  }
}
