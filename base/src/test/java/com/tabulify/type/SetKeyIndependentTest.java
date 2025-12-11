package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class SetKeyIndependentTest {

  @Test
  public void base() {

    Set<String> mapKey = new SetKeyIndependent<>();
    boolean modified = false;
    modified = mapKey.add("Hallo");
    Assert.assertTrue(modified);
    modified = mapKey.add("Hallo");
    Assert.assertFalse(modified);
    modified = mapKey.add("hallo");
    Assert.assertTrue(modified);
    Assert.assertEquals(1,mapKey.size());


  }
}
