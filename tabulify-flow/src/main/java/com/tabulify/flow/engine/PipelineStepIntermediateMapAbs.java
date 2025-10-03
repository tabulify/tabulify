package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;

/**
 * A map operation (one output, one input)
 */
public abstract class PipelineStepIntermediateMapAbs extends PipelineStepAbs implements PipelineStepIntermediateMap {

  public PipelineStepIntermediateMapAbs(PipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
  }

  @Override
  public PipelineStepProcessingType getProcessingType() {
    // map processing is stream processing
    return PipelineStepProcessingType.STREAM;
  }

}
