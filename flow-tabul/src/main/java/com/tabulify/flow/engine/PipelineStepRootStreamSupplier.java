package com.tabulify.flow.engine;

/**
 * A type interface for all root stream step
 */
public interface PipelineStepRootStreamSupplier extends PipelineStepRoot {


  /**
   * Poll the source to create data resources
   */
  void poll();

}
