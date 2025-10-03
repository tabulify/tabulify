package com.tabulify.flow.stream;


import com.tabulify.spi.DataPath;

/**
 * A reduce operation on a {@link PipelineStream pipeline}
 */
public interface PipelineReduceOperation {


  /**
   * Returns the identity (initial value) for the reduction
   * (ie create the collector)
   */
  DataPath identity();

  /**
   * Accumulator function: adds a DataPath element to the collector
   *
   * @param collector - the collector created with the {@link #identity()}
   * @param element   - the element of the stream
   */
  DataPath accumulator(DataPath collector, DataPath element);

  /**
   * Combiner function: combines two collectors into one
   */
  DataPath combiner(DataPath collector1, DataPath collector2);

}
