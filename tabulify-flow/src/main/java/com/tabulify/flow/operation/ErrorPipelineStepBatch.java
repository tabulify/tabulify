package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ErrorPipelineStepBatch extends PipelineStepIntermediateManyToManyAbs {

  private Deque<DataPath> dataPathQueue = new ArrayDeque<>();
  private final ErrorPipelineStep errorBuilder;
  /**
   * A counter and not the size of the queue
   * because the queue can be reset to zero
   */
  private int counter;

  public ErrorPipelineStepBatch(ErrorPipelineStep pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.errorBuilder = pipelineStepBuilder;
  }

  @Override
  public void reset() {
    this.dataPathQueue = new ArrayDeque<>();
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return new ArrayList<>(this.dataPathQueue);
  }


  @Override
  public void accept(DataPath dataPath) {
    this.counter++;
    failProcessing(ErrorPipelineStepFailPoint.INPUT);
    this.dataPathQueue.add(dataPath);
  }

  private void failProcessing(ErrorPipelineStepFailPoint failPoint) {
    if (this.counter >= this.errorBuilder.failEveryNCount && this.errorBuilder.failPoint == failPoint) {
      this.counter = 0;
      throw new ErrorPipelineStepException();
    }
  }

  @Override
  public PipelineStepSupplierDataPath get() {
    failProcessing(ErrorPipelineStepFailPoint.OUTPUT);
    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPaths(dataPathQueue)
      .setIntermediateSupplier(this)
      .build();
  }

}
