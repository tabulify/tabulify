package net.bytle.vertx;

public class VertxRoutingFailureException extends Exception {


  private final VertxRoutingFailureData contextFailure;

  public VertxRoutingFailureException(VertxRoutingFailureData vertxRoutingFailureData) {
    this.contextFailure = vertxRoutingFailureData;
  }


  public VertxRoutingFailureData getFailureContext() {
    return this.contextFailure;
  }

}
