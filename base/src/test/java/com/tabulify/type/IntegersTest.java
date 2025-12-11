package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

public class IntegersTest {

  @Test
  public void toBaseTest() {

    long test = Integers.createFromInteger(0).toBase(3);
    Assert.assertEquals(0L, test);

    test = Integers.createFromInteger(1).toBase(3);
    Assert.assertEquals(1L, test);

    test = Integers.createFromInteger(2).toBase(3);
    Assert.assertEquals(2L, test);

    test = Integers.createFromInteger(10).toBase(3);
    Assert.assertEquals(101L, test);

    test = Integers.createFromInteger(10).toBase(2);
    Assert.assertEquals(1010L, test);

  }


}
