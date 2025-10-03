package com.tabulify.flow.engine;

/**
 * This interface represents a consumer that produces a {@link PipelineStepSupplierDataPath}
 * * {@link PipelineStepIntermediateManyToManyAbs} is a Consumer<DataPath>, Supplier<PipelineStepSupplierDataPathConsumerAbs>
 * * {@link PipelineStepIntermediateOneToMany} is a Function<DataPath, PipelineStepSupplierDataPathConsumerAbs>
 */
public interface PipelineStepIntermediateSupplier extends PipelineStep, PipelineCascadeNodeSupplierStep {
}
