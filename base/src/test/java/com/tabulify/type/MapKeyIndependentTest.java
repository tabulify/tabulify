package com.tabulify.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MapKeyIndependentTest {

  @Test
  public void baseTest() {

    MapKeyIndependent<Integer> map = new MapKeyIndependent<>();
    String lowercase = "smtp_to";
    map.put(lowercase, 1);
    Assertions.assertEquals(1, map.size());
    String uppercase = "SMTP_TO";
    map.put(uppercase, 1);
    Assertions.assertEquals(1, map.size());
    String javaCase = "smtpTo";
    map.put(javaCase, 1);
    Assertions.assertEquals(1, map.size());
    Assertions.assertEquals((Integer) 1, map.get(uppercase));
    Assertions.assertEquals((Integer) 1, map.get(lowercase));

    // contains test
    Assertions.assertTrue(map.containsKey("smtpTo"));
    Assertions.assertTrue(map.containsKey("smtp_To"));

    Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
    Assertions.assertEquals(javaCase, entry.getKey());

    /**
     * Put all
     */
    Map<String, Integer> collection = new HashMap<>();
    collection.put(lowercase, 1);
    map.putAll(collection);
    Assertions.assertEquals(1, map.size());


  }
}
