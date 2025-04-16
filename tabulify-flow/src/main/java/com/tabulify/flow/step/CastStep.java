package com.tabulify.flow.step;

import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.fs.textfile.FsTextDataPathAttributes;
import com.tabulify.spi.DataPath;
import net.bytle.exception.NoVariableException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Feature / if the type given has attribute is text,
 * we give the data resource back as text if this is a text file
 * <p>
 * This is used for instance to be able to print a CSV or a generator not in a tabular format
 * but as text
 */
public class CastStep extends FilterStepAbs implements Function<DataPath, DataPath>, Consumer<DataPath> {

  public static CastStep create() {
    return new CastStep();
  }


  @Override
  public DataPath apply(DataPath dataPath) {


    if (dataPath.getMediaType().isText()) {
      if (dataPath instanceof FsTextDataPath) {
        FsTextDataPath fsDataPath = (FsTextDataPath) dataPath;
        FsTextDataPath casted = FsTextDataPath.create(fsDataPath.getConnection(), fsDataPath.getNioPath());
        for (FsTextDataPathAttributes attribute : FsTextDataPathAttributes.values()) {
          try {
            casted.addVariable(dataPath.getVariable(attribute));
          } catch (NoVariableException e) {
            // not present
          }
        }
        return casted;

      }
    }

    return dataPath;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new CastRunnable(this);
  }

  @Override
  public String getOperationName() {
    return "cast";
  }


  @Override
  public void accept(DataPath dataPath) {
    apply(dataPath);
  }

  private static class CastRunnable implements FilterRunnable {
    private final CastStep castOperation;
    private final Set<DataPath> inputs = new HashSet<>();
    private Set<DataPath> outputs;
    private boolean isDone;

    public CastRunnable(CastStep castOperation) {
      this.castOperation = castOperation;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.outputs = inputs.stream().map(this.castOperation).collect(Collectors.toSet());
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
