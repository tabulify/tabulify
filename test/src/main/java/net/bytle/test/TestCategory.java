package net.bytle.test;

public enum TestCategory {

  /**
   * If a property is set with true, the slow test will run
   *
   *
   */
  SLOW_TEST;

  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}
