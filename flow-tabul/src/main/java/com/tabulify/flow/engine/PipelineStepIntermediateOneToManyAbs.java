package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;

public abstract class PipelineStepIntermediateOneToManyAbs extends PipelineStepAbs implements PipelineStepIntermediateOneToMany {

  public PipelineStepIntermediateOneToManyAbs(PipelineStepBuilder stepBuilder) {
    super(stepBuilder);
  }

  @Override
  public PipelineStepProcessingType getProcessingType() {
    // map processing
    return PipelineStepProcessingType.STREAM;
  }

}
