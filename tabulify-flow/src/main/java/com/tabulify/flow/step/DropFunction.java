package com.tabulify.flow.step;

import com.tabulify.DbLoggers;
import com.tabulify.connection.Connection;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import net.bytle.type.Strings;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

public class DropFunction extends FilterStepAbs implements Function<Set<DataPath>, Set<DataPath>> {


  private boolean withForce = false;


  public DropFunction() {

    this.getOrCreateArgument(DropFunctionArgument.WITH_FORCE).setValueProvider(() -> this.withForce);

  }

  public static DropFunction create() {
    return new DropFunction();
  }

  @Override
  public Set<DataPath> apply(Set<DataPath> sourceDataPaths) {


    if (sourceDataPaths.isEmpty()) {
      DbLoggers.LOGGER_DB_ENGINE.warning("No resources to drop");
      return new HashSet<>();
    }

    /**
     * Data Path may be from different connections
     */
    Map<Connection, List<DataPath>> dataPathsByDataStores = sourceDataPaths
      .stream()
      .collect(
        groupingBy(
          DataPath::getConnection,
          mapping(dp -> dp, toList())
        )
      );

    /**
     * The preparation
     */
    for (Connection connection : dataPathsByDataStores.keySet()) {
      List<DataPath> dataPathByDataStore = dataPathsByDataStores.get(connection);
      for (DataPath dataPathToDrop : ForeignKeyDag.createFromPaths(dataPathByDataStore).getDropOrdered()) {

        /**
         * Exported/Foreign/Reference Table verification
         */
        List<ForeignKeyDef> referenceDataPaths = Tabulars.getReferences(dataPathToDrop);
        for (ForeignKeyDef exportedForeignKeyDef : referenceDataPaths) {
          if (!dataPathByDataStore.contains(exportedForeignKeyDef.getRelationDef().getDataPath())) {
            if (withForce) {

              Tabulars.dropConstraint(exportedForeignKeyDef);
              DbLoggers.LOGGER_DB_ENGINE.warning("ForeignKey (" + exportedForeignKeyDef.getName() + ") was dropped from the table (" + exportedForeignKeyDef.getRelationDef().getDataPath() + ")");

            } else {

              throw new RuntimeException(
                Strings.createMultiLineFromStrings("The table (" + exportedForeignKeyDef + ") is referencing the table (" + dataPathToDrop + ") and is not in the tables to drop",
                  "To drop the foreign keys referencing the tables to drop, you can add the force flag."
                ).toString()
              );
            }
          }
        }
      }

      /**
       * Drop of several data resources
       * should happens at once because of the dependencies
       * Same logical than the truncate
       */
      Tabulars.drop(dataPathByDataStore);

    }

    return sourceDataPaths;

  }

  public DropFunction setWithForce(Boolean withForce) {
    this.withForce = withForce;
    return this;
  }

  @Override
  public String getOperationName() {
    return "drop";
  }


  @Override
  public FilterRunnable createRunnable() {
    return new DropFilterRunnable(this);
  }

  static class DropFilterRunnable implements FilterRunnable {

    private final DropFunction dropFunction;
    private final Set<DataPath> inputs = new HashSet<>();
    private Set<DataPath> dataPaths;
    private boolean isDone = false;

    public DropFilterRunnable(DropFunction dropFunction) {
      this.dropFunction = dropFunction;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.dataPaths = this.dropFunction.apply(inputs);
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
      return dataPaths;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }
}
