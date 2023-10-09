package net.bytle.db.flow.step;

import net.bytle.db.diff.DataPathDataComparison;
import net.bytle.db.flow.FlowLog;
import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.model.ColumnAttribute;
import net.bytle.db.spi.AttributeProperties;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.exception.CastException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.Casts;
import net.bytle.type.Key;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.bytle.db.flow.step.CompareStepSource.CONTENT;

public class CompareStep extends TargetFilterStepAbs {


  private List<String> driverColumns;
  private CompareStepReportType report = CompareStepReportType.RESOURCE;
  private CompareStepSource source = CONTENT;

  public CompareStep() {

    this.getOrCreateArgument(CompareStepArgument.SOURCE).setValueProvider(()->this.source);
    this.getOrCreateArgument(CompareStepArgument.REPORT).setValueProvider(()->this.report);
    this.getOrCreateArgument(CompareStepArgument.DRIVER_COLUMNS).setValueProvider(()->this.driverColumns);


  }

  public static CompareStep create() {
    return new CompareStep();
  }

  @Override
  public String getOperationName() {
    return "compare";
  }


  @Override
  public FilterRunnable createRunnable() {
    return new CompareRunnable(this);
  }

  public CompareStep setDriverColumns(List<String> driverColumns) {
    this.driverColumns = driverColumns;
    return this;
  }

  public CompareStep setReport(CompareStepReportType report) {
    this.report = report;
    return this;
  }

  public CompareStep setSource(CompareStepSource source) {
    this.source = source;
    return this;
  }


  private class CompareRunnable implements FilterRunnable {

    private final CompareStep compareOperation;
    private final Set<DataPath> inputs = new HashSet<>();
    private final Set<DataPath> feedbackDataPaths = new HashSet<>();
    private boolean isDone = false;

    public CompareRunnable(CompareStep compareOperation) {
      this.compareOperation = compareOperation;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {


      DataPath comparisonReport = tabular.getMemoryDataStore().getAndCreateRandomDataPath()
        .setLogicalName("comparison_report")
        .setDescription("Comparison Report")
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

              FlowLog.LOGGER.info("Data Comparison started between the source (" + source + ") and the target (" + target + ")");
              comp = DataPathDataComparison.create(source, target);
              if (driverColumns.size() != 0) {
                comp.setUniqueColumns(driverColumns.toArray(new String[0]));
              }
              comp.compareData();
              break;

            case STRUCTURE:
              FlowLog.LOGGER.info("Structure Comparison started between the source (" + source + ") and the target (" + target + ")");
              ColumnAttribute driverAttribute = ColumnAttribute.NAME;
              if (driverColumns.size() != 0) {
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
                .setUniqueColumns(Key.toColumnName(driverAttribute.toString()))
                .compareData();
              break;
            case ATTRIBUTE:
              FlowLog.LOGGER.info("Attributes Comparison started between the source (" + source + ") and the target (" + target + ")");
              comp =
                DataPathDataComparison.create(source
                      .toVariablesDataPath(),
                    target
                      .toVariablesDataPath())
                  .setUniqueColumns(AttributeProperties.ATTRIBUTE.toString())
                  .compareData();
              break;
            default:
              throw new IllegalArgumentException("The source compare operation (" + this.compareOperation.source + ") should get a processing function associated");
          }

          if (Arrays.asList(CompareStepReportType.ALL, CompareStepReportType.RECORD).contains(report)) {
            feedbackDataPaths.add(comp.getResultDataPath());
          }

          /**
           * We may test after the execution and
           * we creating documentation, we get also an error
           * In dev, no errors if the source and target are not the same
           */
          if (!comp.areEquals() && !JavaEnvs.IS_DEV) {
            tabular.setExitStatus(1);
          }

          insertStream.insert(source, target, comp.areEquals(), comp.getDiffCount(), comp.getRecordCount());
        });
      }

      if (Arrays.asList(CompareStepReportType.ALL, CompareStepReportType.RESOURCE).contains(report)) {
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
    public Set<DataPath> get() throws InterruptedException, ExecutionException {
      return feedbackDataPaths;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }
}
