package com.tabulify.type;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class KeyNormalizers {

  /**
   * Utility function to return a set of normalized key from an enum
   */
  static public Set<KeyNormalizer> getNormalizedNames(Class<? extends Enum<? extends KeyInterface>> enumClass) {
    return Arrays.stream(enumClass.getEnumConstants()).map(KeyNormalizer::createSafe).collect(Collectors.toSet());
  }

}
