package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;

public abstract class PipelineStepRootBatchSupplierAbs extends PipelineStepSupplierDataPathAbs implements PipelineStepRootBatchSupplier {

  public PipelineStepRootBatchSupplierAbs(PipelineStepBuilder stepBuilder) {
    super(stepBuilder);
  }

  @Override
  public PipelineStepProcessingType getProcessingType() {
    return PipelineStepProcessingType.BATCH;
  }

  /**
   * Most of the supplier are not created by Intermediate
   * Only {@link com.tabulify.flow.operation.DefinePipelineStep}
   */
  @Override
  public PipelineStepIntermediateSupplier getIntermediateSupplier() {
    return null;
  }

}
