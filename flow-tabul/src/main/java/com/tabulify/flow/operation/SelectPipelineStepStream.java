package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepRootStreamSupplierAbs;
import com.tabulify.spi.DataPath;

import java.util.ArrayDeque;

public class SelectPipelineStepStream extends PipelineStepRootStreamSupplierAbs {

  private final ArrayDeque<DataPath> dataPathQueue = new ArrayDeque<>();
  private final SelectPipelineStep builder;

  public SelectPipelineStepStream(SelectPipelineStep selectPipelineStep) {
    super(selectPipelineStep);
    this.builder = selectPipelineStep;
  }

  @Override
  public boolean hasNext() {
    return !dataPathQueue.isEmpty();
  }

  @Override
  public DataPath get() {
    return dataPathQueue.poll();
  }

  @Override
  public void poll() {
    dataPathQueue.addAll(builder.produceDataPath());
  }
}
