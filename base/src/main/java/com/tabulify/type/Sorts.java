package com.tabulify.type;

import java.util.Comparator;

public class Sorts {

  /**
   * Sort in Natural Order (ie human)
   * ie  1-foo, 2-bar, 10-foo
   *
   * @param s1         - the first string
   * @param s2         - the second string
   * @param ignoreCase - Ignore case or not
   * @return the natural sort comparison
   * Based on <a href="https://stackoverflow.com/a/26884326/297420">...</a>
   * <p>
   * By default, Java use the {@link Comparator#naturalOrder()} that is not natural order
   * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html#naturalOrder--">...</a>
   */
  public static int naturalSortComparator(String s1, String s2, boolean ignoreCase) {
    if (ignoreCase) {
      s1 = s1.toLowerCase();
      s2 = s2.toLowerCase();
    }
    int s1Length = s1.length();
    int s2Length = s2.length();
    int minSize = Math.min(s1Length, s2Length);
    char s1Char, s2Char;
    boolean s1Number, s2Number;
    boolean asNumeric = false;
    int lastNumericCompare = 0;
    for (int i = 0; i < minSize; i++) {
      s1Char = s1.charAt(i);
      s2Char = s2.charAt(i);
      s1Number = s1Char >= '0' && s1Char <= '9';
      s2Number = s2Char >= '0' && s2Char <= '9';
      if (asNumeric)
        if (s1Number && s2Number) {
          if (lastNumericCompare == 0)
            lastNumericCompare = s1Char - s2Char;
        } else if (s1Number)
          return 1;
        else if (s2Number)
          return -1;
        else if (lastNumericCompare == 0) {
          if (s1Char != s2Char)
            return s1Char - s2Char;
          asNumeric = false;
        } else
          return lastNumericCompare;
      else if (s1Number && s2Number) {
        asNumeric = true;
        lastNumericCompare = s1Char - s2Char;
      } else if (s1Char != s2Char)
        return s1Char - s2Char;
    }
    if (asNumeric)
      if (s1Length > s2Length && s1.charAt(s2Length) >= '0' && s1.charAt(s2Length) <= '9') // as number
        return 1;  // a has bigger size, thus b is smaller
      else if (s2Length > s1Length && s2.charAt(s1Length) >= '0' && s2.charAt(s1Length) <= '9') // as number
        return -1;  // b has bigger size, thus a is smaller
      else if (lastNumericCompare == 0)
        return s1Length - s2Length;
      else
        return lastNumericCompare;
    else
      return s1Length - s2Length;
  }

  public static int naturalSortComparator(String s, String s1) {
    return naturalSortComparator(s, s1, true);
  }

}
