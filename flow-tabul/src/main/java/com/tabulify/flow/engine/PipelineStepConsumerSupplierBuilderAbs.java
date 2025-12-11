package com.tabulify.flow.engine;


import com.tabulify.exception.InternalException;

/**
 * This class represents a step builder build by
 * a {@link PipelineStepIntermediateSupplier child step} (ie not a {@link PipelineStepSupplierDataPath})
 * <p>
 * When building a supplier, it's also a step but should not be instantiated
 * by a user but by the step itself
 * This is a utility that overwrite the build the constructor to send an error
 * if this is the case.
 */
public abstract class PipelineStepConsumerSupplierBuilderAbs extends PipelineStepBuilder {


  @Override
  public PipelineStepBuilder createStepBuilder() {
    /**
     * {@link PipelineStepBuilder} is loaded via {@link PipelineStepBuilder#loadInstalledProviders()}
     * but this one is internal
     */
    throw new InternalException(this.getClass().getSimpleName() + " is an internal operation and should not be created via the service loader");
  }

}
