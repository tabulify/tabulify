package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepRootStreamSupplierAbs;
import com.tabulify.spi.DataPath;

import java.util.ArrayDeque;

public class DefinePipelineStepStream extends PipelineStepRootStreamSupplierAbs {

  private final DefinePipelineStep.DefinePipelineStepBuilder defineBuilder;
  private final ArrayDeque<DataPath> dataPaths = new ArrayDeque<>();

  public DefinePipelineStepStream(DefinePipelineStep.DefinePipelineStepBuilder definePipelineStepBuilder) {
    super(definePipelineStepBuilder);
    this.defineBuilder = definePipelineStepBuilder;
  }

  @Override
  public void poll() {
    dataPaths.addAll(this.defineBuilder.dataPaths);
  }

  @Override
  public boolean hasNext() {
    return !dataPaths.isEmpty();
  }

  @Override
  public DataPath get() {
    return dataPaths.poll();
  }

}
