package com.tabulify.flow.engine;

/**
 * Represents a class that supplies a {@link PipelineStepSupplierDataPath}
 * to be used as supplier in a {@link PipelineCascadeNode}
 * <p>
 * It merges :
 * * a {@link PipelineStepRoot} that is a {@link PipelineStepSupplierDataPath}
 * * a {@link PipelineStepIntermediateSupplier} that produces a {@link PipelineStepSupplierDataPath}
 */
public interface PipelineCascadeNodeSupplierStep extends PipelineStep {

}
