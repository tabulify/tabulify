package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepRootBatchSupplier;
import com.tabulify.type.KeyNormalizer;

/**
 * Note: In a stream pipeline (define by the {@link PipelineStepRootBatchSupplier root supplier process type},
 * we can't have a batch step
 * while the inverse is not true
 * <p>
 * An intermediate step may choose to implement the processing as:
 * * a {@link  com.tabulify.flow.engine.PipelineStepIntermediateMap} map
 * * or aggregate (batch) a {@link  com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs} map
 * (ie 1 on 1 or many to once)
 * A good example is the transfer step:
 * * if you do a concat (many source, one target), you want a batch to gather the sources
 * * otherwise you want a one on one
 */
public enum PipelineStepProcessingType {

  /**
   * For a root supplier step, the number of elements returned are finite (ie end when there is no elements anymore)
   * For an intermediate step, collect all sources and process at the end
   */
  BATCH,
  /**
   * For a root supplier step, the number of elements returned are infinite (ie poll continuously at interval)
   * For a intermediate step, perform the operation immediately (ie {@link com.tabulify.flow.engine.PipelineStepIntermediateMap}
   */
  STREAM;


  @Override
  public String toString() {
    return KeyNormalizer.createSafe(this.name()).toCliLongOptionName();
  }
}
