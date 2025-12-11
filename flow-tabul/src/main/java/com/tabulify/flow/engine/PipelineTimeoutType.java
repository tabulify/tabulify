package com.tabulify.flow.engine;

public enum PipelineTimeoutType {

  /**
   * A max duration has been asked
   * (Not an error)
   */
  DURATION,
  /**
   * A timeout has been asked
   * (An error)
   */
  ERROR

}
