package com.tabulify.flow.stream;

import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * An iterator for stream
 * that returns always the specified token (ie not infinite)
 */
public class PipelineSpliterator extends Spliterators.AbstractSpliterator<DataPath> {


  private final PipelineStepSupplierDataPath dataPathStreamSupplier;


  protected PipelineSpliterator(PipelineStepSupplierDataPath dataPathStreamSupplier) {
    super(dataPathStreamSupplier.hasNext() ? 1L : 0L, Spliterator.SIZED);
    this.dataPathStreamSupplier = dataPathStreamSupplier;
  }


  /**
   * @param streamEngine The stream
   */
  @Override
  public boolean tryAdvance(Consumer<? super DataPath> streamEngine) {
    DataPath t = dataPathStreamSupplier.get();
    if (t == null) {
      return false;
    }
    streamEngine.accept(t);
    return true;
  }

}
