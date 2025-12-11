package com.tabulify.jdbc;

import java.util.HashMap;
import java.util.Map;

class BaseClass {
}

class SubClass1 extends BaseClass {
}

class SubClass2 extends BaseClass {
}

public class GenericMapExample {
  // This works fine - different type parameters for key and value
  public <K extends BaseClass, V extends BaseClass> void process(Map<K, V> map) {
    for (Map.Entry<K, V> entry : map.entrySet()) {
      BaseClass key = entry.getKey();    // Safe because K extends BaseClass
      BaseClass value = entry.getValue(); // Safe because V extends BaseClass
    }
  }

  // This also works - same type for both key and value
  public <T extends BaseClass> void processSameType(Map<T, T> map) {
    for (Map.Entry<T, T> entry : map.entrySet()) {
      T key = entry.getKey();
      T value = entry.getValue();
      // Both are of type T which extends BaseClass
    }
  }

  public void usage() {
    Map<SubClass1, SubClass2> map1 = new HashMap<>();
    process(map1); // Works with the first method

    Map<SubClass1, SubClass1> map2 = new HashMap<>();
    process(map2);         // Works with first method
    processSameType(map2); // Works with second method
  }
}
