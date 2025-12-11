package com.tabulify.flow.engine;

public enum PipelineOnErrorAction {

  /**
   * Park, move the bad data resource
   * out of the stream
   */
  PARK,
  /**
   * Discard the data resource
   * and continue
   * Error counter is still updated
   */
  DISCARD,
  /**
   * Stop the stream
   */
  STOP

}
