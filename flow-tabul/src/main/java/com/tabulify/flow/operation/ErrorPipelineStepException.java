package com.tabulify.flow.operation;

public class ErrorPipelineStepException extends RuntimeException {

  public ErrorPipelineStepException() {
  }

  /**
   * Still used via reading the meta
   * with {@link com.tabulify.exception.ExceptionWrapper}
   */
  @SuppressWarnings("unused")
  public ErrorPipelineStepException(String message, Throwable cause) {
    super(message, cause);
  }

}
