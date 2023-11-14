package net.bytle.vertx;

public class VertxFailureHttpException extends Exception {


  private final VertxFailureHttp contextFailure;

  public VertxFailureHttpException(VertxFailureHttp vertxFailureHttp) {
    this.contextFailure = vertxFailureHttp;
  }


  public VertxFailureHttp getFailureContext() {
    return this.contextFailure;
  }

}
