package net.bytle.test;

import static org.junit.Assume.assumeTrue;

public class TestUtil {

  public static void runOnlyIfSlowTestisOn() {
    assumeTrue("true".equals(System.getProperty(TestCategory.SLOW_TEST.toString())));
  }

}
