package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;

public abstract class PipelineStepRootStreamSupplierAbs extends PipelineStepSupplierDataPathAbs implements PipelineStepRootStreamSupplier {


  public PipelineStepRootStreamSupplierAbs(PipelineStepBuilderStreamSupplier stepBuilder) {
    super(stepBuilder);
  }

  @Override
  public PipelineStepProcessingType getProcessingType() {
    return PipelineStepProcessingType.STREAM;
  }

  @Override
  public PipelineStepIntermediateSupplier getIntermediateSupplier() {
    // not supplied
    // A stream supplier cannot be created by an intermediate
    // We return null because this is the convention
    return null;
  }


}
