package net.bytle.vertx.resilience;

public enum ValidationTestStatus {

  /**
   * The test got a fatal error
   * (timeout, ...)
   */
  FATAL_ERROR,
  /**
   * The test was successful
   */
  PASS,
  /**
   * The test has failed
   */
  FAILED,
  /**
   * Skipped is a special status
   * that indicates that a test has been
   * skipped (mostly because it depends
   * on the good execution of another test)
   * For instance, you can't check a website certificate,
   * if a DNS A record test has failed
   */
  SKIPPED
}
