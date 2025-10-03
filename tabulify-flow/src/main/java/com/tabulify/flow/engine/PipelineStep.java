package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.flow.operation.TransferPipelineStepStream;
import net.bytle.type.KeyNormalizer;

/**
 * The metadata of a pipeline step
 */
public interface PipelineStep extends ExecutionNode {


  /**
   * @return the operation name
   */
  KeyNormalizer getOperationName();

  /**
   *
   * @return the {@link Pipeline}
   */
  Pipeline getPipeline();


  /**
   * The step number in the pipeline step list
   */
  Integer getPipelineStepId();

  /**
   * @return The processing type of the step
   * <p>
   * Note that because of the builder pattern, the processing type
   * may be changed at runtime.
   * Example:
   * {@link TransferPipelineStepStream} may produce:
   * * a {@link PipelineStepIntermediateManyToManyAbs}
   * * or a {@link PipelineStepIntermediateMap}
   */
  PipelineStepProcessingType getProcessingType();

}
