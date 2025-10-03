package com.tabulify.flow.engine;

import com.tabulify.spi.DataPath;

import java.util.function.Supplier;

/**
 * A class that represents a supplier (ie a data path supplier) that gives back a data path
 */
public interface PipelineStepSupplierDataPath extends Supplier<DataPath>, PipelineCascadeNodeSupplierStep {


  /**
   * @return if there is another element
   */
  boolean hasNext();

  /**
   * @return the pipeline consumer (ie the step that created this pipeline, the parent)
   * May be null
   */
  PipelineStepIntermediateSupplier getIntermediateSupplier();

}
