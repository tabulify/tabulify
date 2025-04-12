package com.tabulify.flow.step;

import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import net.bytle.type.Strings;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extract the head element of source into target for a size of limit
 */
public class HeadFunction extends FilterStepAbs implements Function<DataPath, DataPath> {


  public HeadFunction() {

    this.getOrCreateArgument(HeadFunctionArgument.LIMIT).setValueProvider(() -> this.limit);

  }

  /**
   * the number of element returned
   */
  private Integer limit = 10;


  public static HeadFunction create() {
    return new HeadFunction();
  }

  @Override
  public DataPath apply(DataPath source) {


    DataPath target = source.getConnection().getTabular().getMemoryDataStore().getDataPath("head_" + source.getLogicalName())
      .setDescription("The first " + limit + " rows of the data resource (" + source + "): ");

    target.getOrCreateRelationDef().copyStruct(source);

    // Head
    try (
      SelectStream selectStream = source.getSelectStream();
      InsertStream insertStream = target.getInsertStream()
    ) {
      RelationDef sourceDataDef = selectStream.getRuntimeRelationDef();
      if (sourceDataDef.getColumnsSize() == 0) {
        // No row structure even at runtime
        throw new RuntimeException(Strings.createMultiLineFromStrings(
            "The data path (" + source + ") has no row structure. ",
            "To extract a head, a row structure is needed.",
            "Tip for intern developer: if it's a text file, create a line structure (one row, one cell with one line)")
          .toString());
      }

      int i = 0;
      while (selectStream.next() && i < limit) {
        i++;
        insertStream.insert(selectStream.getObjects());
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }

    return target;

  }

  public HeadFunction setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public String getOperationName() {
    return "head";
  }


  @Override
  public FilterRunnable createRunnable() {
    return new HeadFunctionFilterRunnable(this);
  }

  private static class HeadFunctionFilterRunnable implements FilterRunnable {
    private final HeadFunction headFunction;
    private final Set<DataPath> inputs = new HashSet<>();
    private Set<DataPath> outputs;
    private boolean isDone = false;

    public HeadFunctionFilterRunnable(HeadFunction headFunction) {
      this.headFunction = headFunction;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.outputs = this.inputs.stream()
        .map(this.headFunction)
        .collect(Collectors.toSet());
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
      return outputs;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }
}
