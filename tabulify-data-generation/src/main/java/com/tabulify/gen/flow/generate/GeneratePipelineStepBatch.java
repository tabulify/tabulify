package com.tabulify.gen.flow.generate;

import com.tabulify.flow.engine.PipelineStepRootBatchSupplierAbs;
import com.tabulify.spi.DataPath;

import java.util.ArrayDeque;

public class GeneratePipelineStepBatch extends PipelineStepRootBatchSupplierAbs {

  private final GeneratePipelineStep generateBuilder;

  private final ArrayDeque<DataPath> dataPathQueue = new ArrayDeque<>();

  public GeneratePipelineStepBatch(GeneratePipelineStep generatePipelineStep) {
    super(generatePipelineStep);
    this.generateBuilder = generatePipelineStep;
  }

  @Override
  public void onStart() {

    dataPathQueue.addAll(this.generateBuilder.getGenDataPaths());

  }

  @Override
  public boolean hasNext() {
    return !dataPathQueue.isEmpty();
  }


  @Override
  public DataPath get() {
    return dataPathQueue.poll();
  }

}
