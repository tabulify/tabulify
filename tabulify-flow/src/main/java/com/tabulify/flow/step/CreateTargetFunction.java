package com.tabulify.flow.step;

import com.tabulify.DbLoggers;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Create the target from inputs sources
 * and return a data path with the source and the target
 */
public class CreateTargetFunction extends TargetFilterStepAbs
  implements Function<Set<DataPath>, DataPath>, Consumer<Set<DataPath>> {

  public static CreateTargetFunction create() {
    return new CreateTargetFunction();
  }

  @Override
  public DataPath apply(Set<DataPath> sources) {


    return toDataPath(createTarget(sources));

  }

  /**
   * soure target data path
   */
  private DataPath toDataPath(Map<DataPath, DataPath> target) {
    try (InsertStream insertStream = tabular.getMemoryDataStore().getAndCreateRandomDataPath()
      .setLogicalName("source_target")
      .createRelationDef()
      .addColumn("source")
      .addColumn("target")
      .getDataPath()
      .getInsertStream()) {
      target.forEach((key, value) -> insertStream.insert(key.toDataUri().toString(), value.toDataUri().toString()));
      return insertStream.getDataPath();
    }
  }

  @Override
  public void accept(Set<DataPath> sources) {

    createTarget(sources);

  }

  private Map<DataPath, DataPath> createTarget(Set<DataPath> sources) {

    Map<DataPath, DataPath> sourceTargets = this.getSourceTarget(sources);


    /**
     * Doing the work
     */
    List<DataPath> dataPathsToCreate = ForeignKeyDag.createFromPaths(sourceTargets.keySet()).getCreateOrdered();
    for (DataPath source : dataPathsToCreate) {

      DataPath target = sourceTargets.get(source);

      /**
       * DataDef copy
       */
      target
        .getOrCreateRelationDef()
        .mergeDataDef(source)
      ;

      /**
       * Creation
       */
      if (Tabulars.exists(target)) {
        throw new IllegalArgumentException("The data resources (" + target.getName() + ") exists already in the data store (" + target.getConnection().getName() + ") and was not created");
      } else {

        Tabulars.create(target);

        DbLoggers.LOGGER_DB_ENGINE.fine("The data resource (" + target + ") was created");

      }
    }

    return sourceTargets;
  }


  @Override
  public String getOperationName() {
    return "createTarget";
  }


  @Override
  public FilterRunnable createRunnable() {
    return new CreateTargetFilterRunnable(this);
  }


  private static class CreateTargetFilterRunnable implements FilterRunnable {
    private final CreateTargetFunction createTargetAndGetSourceMap;
    private final Set<DataPath> inputs = new HashSet<>();
    private boolean isDone;
    private DataPath output;

    public CreateTargetFilterRunnable(CreateTargetFunction createTargetAndGetSourceMap) {
      this.createTargetAndGetSourceMap = createTargetAndGetSourceMap;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.output = this.createTargetAndGetSourceMap.toDataPath(this.createTargetAndGetSourceMap.createTarget(inputs));
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
      return Collections.singleton(this.output);
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) {
      return get();
    }
  }
}
