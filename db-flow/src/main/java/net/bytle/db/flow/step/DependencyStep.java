package net.bytle.db.flow.step;

import net.bytle.dag.Dependency;
import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.stream.InsertStream;
import net.bytle.type.Key;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class DependencyStep extends FilterStepAbs {

  public static OperationStep create() {
      return new DependencyStep();
  }

  @Override
  public String getOperationName() {
    return "dependency";
  }



  @Override
  public boolean isAccumulator() {
    return true;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new DependencyFilterRunnable();
  }


  private class DependencyFilterRunnable implements FilterRunnable {

    private final Set<DataPath> inputs = new HashSet<>();
    private boolean isDone = false;
    private RelationDef feedback;



    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {


      // Creating a table to use the print function
      feedback = tabular.getMemoryDataStore().getAndCreateRandomDataPath()
        .setLogicalName("dependencies")
        .getOrCreateRelationDef();
      feedback.addColumn(Key.toColumnName("Id"), Types.INTEGER);

      feedback
        .addColumn(Key.toColumnName(DataPathAttribute.DATA_URI))
        .addColumn(Key.toColumnName("Dependency"));


      try (
        InsertStream insertStream = feedback.getDataPath().getInsertStream()
      ) {
        // Filling the table with data
        Integer dependenciesNumber = 0;
        for (DataPath dataPath : this.inputs.stream().sorted().collect(Collectors.toList())) {

          for (Dependency dependency : dataPath.getDependencies()) {
            List<Object> row = new ArrayList<>();
            dependenciesNumber++;
            row.add(dependenciesNumber);
            row.add(dataPath.toDataUri().toString());
            row.add(dependency.getId());
            insertStream.insert(row);
          }

        }
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
      return Collections.singleton(feedback.getDataPath());
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }
}
