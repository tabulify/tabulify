package net.bytle.db.flow.step;

import net.bytle.db.engine.ForeignKeyDag;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Create a list of data paths in their system
 */
public class CreateStep extends FilterStepAbs implements OperationStep, Consumer<Set<DataPath>>, Function<Set<DataPath>, Set<DataPath>> {

  public static CreateStep create() {
    return new CreateStep();
  }

  @Override
  public String getOperationName() {
    return "create";
  }


  @Override
  public void accept(Set<DataPath> dataPaths) {

    for (DataPath dataPath : ForeignKeyDag.createFromPaths(dataPaths).getCreateOrdered()) {
      Tabulars.create(dataPath);
    }

  }

  @Override
  public Set<DataPath> apply(Set<DataPath> dataPaths) {
    accept(dataPaths);
    return dataPaths;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new CreateFilterRunnable(this);
  }

  private static class CreateFilterRunnable implements FilterRunnable {
    private final CreateStep createOperation;
    private final Set<DataPath> inputs = new HashSet<>();
    private boolean isDone = false;

    public CreateFilterRunnable(CreateStep createOperation) {
      this.createOperation = createOperation;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.createOperation.accept(inputs);
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
      return this.inputs;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }

  }
}
