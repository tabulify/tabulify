package net.bytle.db.gen.generator;


import java.util.HashMap;
import java.util.Map;

/**
 * A class that helps build a string from an integer
 * where the string representation has no number to avoid sort order problem
 * when asking for the max value (ie the order of 1 may be below A in one character set and above in another one)
 */
public class StringGenerator {

  /**
   * All possible chars for representing a number as a String
   */
  final static char[] digits = {
    'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h',
    'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z'
  };


  final static Map<Character, Integer> digitsMap = new HashMap<>();

  static {
    for (int i = 0; i < digits.length; i++) {
      digitsMap.put(digits[i], i);
    }
  }

  /**
   * The radix is the total number of digits
   * Decimals are for 10 digits from 0 to 9
   */
  public static final int MAX_RADIX = digits.length;
  private static final int MIN_RADIX = 2;

  /**
   * Adaptation of {@link Integer#toString(int, int)}
   *
   * @param i     (Positive Integer)
   * @param modulo - the modulo
   *               - if 1, it will generate a string with only the a characters
   *               - if 2, it will generate a string with the a and b characters
   * @param len - the length of the string
   * @return
   */
  public static String toString(int i, int modulo, int len) {
    if (modulo < MIN_RADIX || modulo > MAX_RADIX)
      modulo = MAX_RADIX;

    if (len > CollectionGeneratorOnce.MAX_STRING_PRECISION) {
      len = CollectionGeneratorOnce.MAX_STRING_PRECISION;
    }
    char buf[] = new char[len];

    int charPos = len - 1; // Array - 1

    while (i >= modulo) {
      buf[charPos--] = digits[(i % modulo)];
      i = i / modulo;
    }
    buf[charPos] = digits[i];

    // fill the blank with the first character (ie 0)
    while (charPos > 0) {
      charPos--;
      buf[charPos] = digits[0];
    }

    return new String(buf, charPos, (len - charPos));
  }


  /**
   * @param s
   * @param radix
   * @return
   */
  public static int toInt(String s, int radix) {

    if (radix < MIN_RADIX) {
      throw new IllegalArgumentException("radix " + radix + " less than MIN_RADIX");
    }

    if (radix > MAX_RADIX) {
      throw new IllegalArgumentException("radix " + radix + " greater than MAX_RADIX");
    }

    int result = 0;
    int i = 0;
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    int len = s.length();
    while (i < len) {

      final char key = s.charAt(i);
      digit = digitsMap.get(key);
      int multi;
      if (i == len - 1) {
        multi = 1;
      } else {
        multi = (len - (i + 1)) * radix;
      }
      result = result + digit * multi;
      i++;

    }

    return result;
  }

}
