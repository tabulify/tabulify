package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

import java.util.List;

public class DependencyPipelineStepStream extends PipelineStepIntermediateMapAbs {

  private final DependencyPipelineStep pipelineStep;

  public DependencyPipelineStepStream(DependencyPipelineStep pipeline) {
    super(pipeline);
    this.pipelineStep = pipeline;
  }


  @Override
  public DataPath apply(DataPath dataPath) {

    return pipelineStep.getDependencyDataPath(List.of(dataPath));

  }


}
