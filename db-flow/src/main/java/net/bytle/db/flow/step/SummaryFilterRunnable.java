package net.bytle.db.flow.step;

import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.spi.DataPath;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SummaryFilterRunnable implements FilterRunnable {

  private final SummaryStep summaryOperation;
  private DataPath summaryDataPath;
  private boolean isDone = true;

  public SummaryFilterRunnable(SummaryStep summaryOperation) {
    this.summaryOperation = summaryOperation;
  }

  @Override
  public void addInput(Set<DataPath> inputs) {
    //noinspection MethodRefCanBeReplacedWithLambda
    inputs.forEach(this.summaryOperation::accept);
  }

  @Override
  public void run() {
    this.summaryDataPath = this.summaryOperation.finisher().apply(summaryOperation);
    this.isDone = true;
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
    return Collections.singleton(this.summaryDataPath);
  }

  @Override
  public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return get();
  }
}
