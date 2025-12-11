package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class KeyTest {

  @Test
  public void splitTest() {

    List<String> names = Key.splitName("_TEST_BIG_");
    Assert.assertEquals(2, names.size());
    Assert.assertEquals("TEST",names.get(0));
    Assert.assertEquals("BIG",names.get(1));

    List<String> camelNames = Key.splitName("TestBig");
    Assert.assertEquals(2, camelNames.size());


  }

  @Test
  public void shortOptionNameTest() {

    String name = Key.toShortOptionName("TEST_BIG");
    Assert.assertEquals("tb", name);

    String camelName = Key.toShortOptionName("TestBig");
    Assert.assertEquals("tb", camelName);

  }

  @Test
  public void longOptionNameTest() {

    String name = Key.toLongOptionName("TEST_BIG");
    Assert.assertEquals("test-big", name);

    String camelName = Key.toLongOptionName("TestBig");
    Assert.assertEquals("test-big", camelName);

  }


}
