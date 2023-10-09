package net.bytle.db.flow.step;

import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Execute a script Data Path
 */
public class ExecuteFunction extends FilterStepAbs implements Function<DataPath, DataPath>, OperationStep {


  private boolean strict = true;


  public static ExecuteFunction create() {
    return new ExecuteFunction();
  }

  public void setStrict(boolean strict) {
    this.strict = strict;
  }


  @Override
  public String getOperationName() {
    return "execute";
  }



  @Override
  public FilterRunnable createRunnable() {
    return new ExecuteFilterRunnable(this);
  }

  @Override
  public boolean isAccumulator() {
    return false;
  }

  /**
   * Functional interface
   *
   */
  @Override
  public DataPath apply(DataPath dataPath) {


    if (Tabulars.isScript(dataPath)) {
      Tabulars.execute(dataPath);

    } else {
      String msg = "The data path (" + dataPath + ") is not a script";
      if (this.strict) {
        throw new RuntimeException(msg);
      }
    }

    return dataPath;

  }

  static public class ExecuteFilterRunnable implements FilterRunnable {

    private final ExecuteFunction executeFunction;
    private final Set<DataPath> inputs = new HashSet<>();
    private Set<DataPath> outputs;
    private boolean isDone = false;

    public ExecuteFilterRunnable(ExecuteFunction executeFunction) {
      this.executeFunction = executeFunction;
    }

    @Override
    public void addInput(Set<DataPath> input) {
      this.inputs.addAll(input);
    }

    @Override
    public void run() {
      this.outputs = inputs
        .stream()
        .map(this.executeFunction)
        .collect(Collectors.toSet());
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
      return this.outputs;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }


}
