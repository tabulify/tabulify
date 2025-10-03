package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

public class ErrorPipelineStepStream extends PipelineStepIntermediateMapAbs {

  private final ErrorPipelineStep errorBuilder;
  private int counter = 0;

  public ErrorPipelineStepStream(ErrorPipelineStep pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.errorBuilder = pipelineStepBuilder;
  }

  @Override
  public DataPath apply(DataPath dataPath) {
    if (!this.errorBuilder.isEnabled) {
      return dataPath;
    }
    this.counter++;
    if (this.counter >= this.errorBuilder.failEveryNCount) {
      this.counter = 0;
      throw new ErrorPipelineStepException();
    }
    return dataPath;
  }
}
