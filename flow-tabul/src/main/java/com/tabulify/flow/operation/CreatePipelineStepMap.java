package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

/**
 * Create the target from sources
 * and return a result data path with the source and the target
 */
public class CreatePipelineStepMap extends PipelineStepIntermediateMapAbs {

  private final CreatePipelineStep builder;


  public CreatePipelineStepMap(CreatePipelineStep createTargetFunctionBuilder) {
    super(createTargetFunctionBuilder);
    this.builder = createTargetFunctionBuilder;
  }


  @Override
  public DataPath apply(DataPath source) {

    return new CreatePipelineStepFunction(this.builder).apply(source);

  }




}
