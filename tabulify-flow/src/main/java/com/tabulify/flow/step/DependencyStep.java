package com.tabulify.flow.step;

import net.bytle.dag.Dependency;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.OperationStep;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import net.bytle.type.KeyNormalizer;

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
      feedback.addColumn(KeyNormalizer.create("Id").toSqlCase(), Types.INTEGER);

      feedback
        .addColumn(KeyNormalizer.create(DataPathAttribute.DATA_URI).toSqlCase())
        .addColumn(KeyNormalizer.create("Dependency").toSqlCase());


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
    public Set<DataPath> get() {
      return Collections.singleton(feedback.getDataPath());
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }
}
