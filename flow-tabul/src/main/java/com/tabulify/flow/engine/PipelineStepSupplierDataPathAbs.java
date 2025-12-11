package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.type.KeyNormalizer;

/**
 * A data path supplier that was produce by a {@link PipelineStepIntermediateSupplier}
 */
public abstract class PipelineStepSupplierDataPathAbs extends PipelineStepAbs implements PipelineStepSupplierDataPath, PipelineCascadeNodeSupplierStep {

  public PipelineStepSupplierDataPathAbs(PipelineStepBuilder stepProvider) {
    super(stepProvider);
  }


  @Override
  public Integer getPipelineStepId() {
    if (getIntermediateSupplier() != null) {
      // In the result, a step consumes and produces
      // that why we set the same id
      return getIntermediateSupplier().getPipelineStepId();
    }
    return super.getPipelineStepId();
  }

  @Override
  public KeyNormalizer getNodeName() {
    if (getIntermediateSupplier() != null) {
      // In the result, a step consumes and produces
      // that why we set the same name
      return getIntermediateSupplier().getNodeName();
    }
    return super.getNodeName();
  }

  @Override
  public Pipeline getPipeline() {
    if (getIntermediateSupplier() != null) {
      // pipeline value is generally injected at build time
      // but if the user forgot, here it's
      return getIntermediateSupplier().getPipeline();
    }
    return super.getPipeline();
  }

  @Override
  public String toString() {
    if (getIntermediateSupplier() != null) {
      // pipeline value is generally injected at build time
      // but if the user forgot, here it's
      return getIntermediateSupplier().toString();
    }
    return super.toString();
  }

  @Override
  public PipelineStepProcessingType getProcessingType() {
    return PipelineStepProcessingType.STREAM;
  }

}
