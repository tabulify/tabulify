package com.tabulify.flow.step;

import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class PrintStep extends FilterStepAbs implements Function<DataPath, DataPath>, Consumer<DataPath> {

  public static PrintStep create() {
    return new PrintStep();
  }

  @Override
  public String getOperationName() {
    return "print";
  }




  @Override
  public FilterRunnable createRunnable() {
    return new FilterRunnable() {
      private boolean isCancelled = false;
      private boolean isDone = false;
      private Set<DataPath> inputs;

      @Override
      public void addInput(Set<DataPath> input) {
        this.inputs = input;
      }

      @Override
      public void run() {
        inputs.forEach(Tabulars::print);
        this.isDone = true;
      }

      @Override
      public Set<DataPath> get() {
        return this.inputs;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        this.isCancelled = true;
        return true;
      }

      @Override
      public boolean isCancelled() {
        return this.isCancelled;
      }

      @Override
      public boolean isDone() {
        return isDone;
      }

      @Override
      public Set<DataPath> get(long timeout, TimeUnit unit) {
        return this.inputs;
      }
    };
  }

  @Override
  public DataPath apply(DataPath dataPath) {
    System.out.println();
    Tabulars.print(dataPath);
    System.out.println();
    return dataPath;
  }

  @Override
  public void accept(DataPath dataPath) {
    apply(dataPath);
  }
}
