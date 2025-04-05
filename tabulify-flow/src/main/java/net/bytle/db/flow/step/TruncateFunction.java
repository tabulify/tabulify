package net.bytle.db.flow.step;

import net.bytle.db.DbLoggers;
import net.bytle.db.connection.Connection;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.type.Strings;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

/**
 * Truncate is a set operation
 */
public class TruncateFunction extends FilterStepAbs implements Function<Set<DataPath>, Set<DataPath>>, Consumer<Set<DataPath>> {

  private boolean withForce = false;


  public static TruncateFunction create() {
    return new TruncateFunction();
  }


  @Override
  public Set<DataPath> apply(Set<DataPath> dataPaths) {

    accept(dataPaths);
    return dataPaths;

  }

  public TruncateFunction setWithForce(Boolean withForce) {
    this.withForce = withForce;
    return this;
  }



  @Override
  public void accept(Set<DataPath> dataPaths) {


    Map<Connection, List<DataPath>> dataPathsByDataStores = dataPaths
      .stream()
      .collect(
        groupingBy(
          DataPath::getConnection,
          mapping(dp -> dp, toList())
        )
      );


    // Doing the work
    for (Connection connection : dataPathsByDataStores.keySet()) {
      List<DataPath> dataPathsByDataStore = dataPathsByDataStores.get(connection);

      /**
       * Check if we have all dependencies
       */
      List<DataPath> dataPathToTruncates = ForeignKeyDag.createFromPaths(dataPathsByDataStore).getDropOrdered();
      for (DataPath dataPathToTruncate : dataPathToTruncates) {

        List<ForeignKeyDef> exportedForeignKeys = Tabulars.getReferences(dataPathToTruncate);
        for (ForeignKeyDef exportedForeignKey : exportedForeignKeys) {
          DataPath exportedDataPath = exportedForeignKey.getRelationDef().getDataPath();
          if (!dataPathsByDataStore.contains(exportedDataPath)) {
            if (withForce) {
              Tabulars.dropConstraint(exportedForeignKey);
              DbLoggers.LOGGER_DB_ENGINE.warning("ForeignKey (" + exportedForeignKey.getName() + ") was dropped from the table (" + exportedDataPath + ")");
            } else {

              String msg = Strings.createMultiLineFromStrings(
                "Unable to truncate the data resource (" + dataPathToTruncate + ")",
                "The table (" + exportedDataPath + ") is referencing the table (" + dataPathToTruncate + ") and is not in the tables to truncate",
                "To resolve, this problem you can:",
                "  * drop the foreign keys referencing the tables to truncate with the force flag.",
                "  * add the primary tables referencing the tables to truncate with the with-dependencies flag."

              ).toString();
              throw new IllegalArgumentException(msg);

            }
          }
        }

      }
      Tabulars.truncate(dataPathToTruncates);
    }
  }

  @Override
  public String getOperationName() {
    return "truncate";
  }



  @Override
  public FilterRunnable createRunnable() {
    return new TruncateRunnable(this);
  }


  private static class TruncateRunnable implements FilterRunnable {
    private final TruncateFunction truncateFunction;
    private final Set<DataPath> inputs = new HashSet<>();
    private boolean isDone = false;

    public TruncateRunnable(TruncateFunction truncateFunction) {
      this.truncateFunction = truncateFunction;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.truncateFunction.accept(inputs);
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
      return inputs;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }
}
