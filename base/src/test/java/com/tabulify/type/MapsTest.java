package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsTest {

  @Test
  public void mapSortedByKeyTest() {
    Map<Integer,String> map = new HashMap<>();
    map.put(2,"2");
    map.put(1,"1");
    Map<Integer, String> mapSorted = Maps.getMapSortByKey(map);
    List<String> sortedValuesList = new ArrayList<>(mapSorted.values());
    Assert.assertEquals("first is first","1",sortedValuesList.get(0));
    Assert.assertEquals("second is second","2",sortedValuesList.get(1));
  }
}
