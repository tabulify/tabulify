package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

public class ExecutePipelineStream extends PipelineStepIntermediateMapAbs {


  protected final ExecutePipelineStep execPipelineStep;


  public ExecutePipelineStream(ExecutePipelineStep execPipelineStep) {
    super(execPipelineStep);
    this.execPipelineStep = execPipelineStep;
  }


  @Override
  public DataPath apply(DataPath dataPath) {

    return this.execPipelineStep.execute(dataPath);

  }


}
