package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class MapBiDirectionalTest {

  @Test
  public void goodTest() {
    MapBiDirectional<Integer, Integer> biMap = new MapBiDirectional<>();
    biMap.put(1,2);
    biMap.put(2,3);
    biMap.put(2,3); // should be able to do that
    biMap.remove(1); //
    biMap.put(1,4);
    biMap.put(0,2);
    biMap.put(3,1);
    Assert.assertEquals("value 1 key is good",(Integer) 3,biMap.inverse().get(1));
    Assert.assertEquals("value 2 key is good",(Integer) 0,biMap.getKey(2));
    Assert.assertEquals("value 3 key is good",(Integer) 2,biMap.getKey(3));
    Assert.assertEquals("value 4 key is good",(Integer) 1,biMap.getKey(4));
  }

  @Test(expected = RuntimeException.class)
  public void badInsertTest() {
    Map<Integer, Integer> biMap = new MapBiDirectional<>();
    biMap.put(1,2);
    biMap.put(2,2);
  }

}
