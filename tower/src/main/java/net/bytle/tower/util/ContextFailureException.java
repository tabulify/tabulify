package net.bytle.tower.util;

public class ContextFailureException extends Throwable {


  private final ContextFailureData contextFailure;

  public ContextFailureException(ContextFailureData contextFailureData) {
    this.contextFailure = contextFailureData;
  }

  public ContextFailureData getFailureContext() {
    return this.contextFailure;
  }

}
