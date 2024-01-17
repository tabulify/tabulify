package net.bytle.vertx.future;

public enum TowerFutureCoordination {

  /**
   * Execute them all,
   * Return at the first failure
   * Fail at the first failure
   */
  ALL,
  /**
   * Execute them all,
   * Return when they are all executed,
   * Fail at the first failure
   */
  JOIN,
}
