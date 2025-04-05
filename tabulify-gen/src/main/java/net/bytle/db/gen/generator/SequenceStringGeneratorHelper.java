package net.bytle.db.gen.generator;


import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that helps build a sequence based on characters
 */
public class SequenceStringGeneratorHelper {

  /**
   * The set of possible characters
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
   * For a simple sequence, use the function {@link #toString(int)}
   * <p>
   * This function will generate a string:
   * * with the length (len)
   * * with the set of characters defined by the modulo
   * * from the value i
   * <p>
   * If the value does not fill the length of the string, the
   * first character (a) is used to fill it
   * <p>
   * Adaptation of {@link Integer#toString(int, int)}
   *
   * @param i      (Positive Integer)
   * @param modulo - the modulo
   *               - if 1, it will generate a string with only the a characters
   *               - if 2, it will generate a string with the a and b characters
   *               - if 3, it will generate a string with the set of characters (a, b, c)
   *               - until z ...
   * @return TODO: {@link CollectionGenerator#MAX_STRING_PRECISION} check in case that the length goes out of the roof ?
   */
  public static String toString(int i, int modulo) {

    if (modulo < MIN_RADIX || modulo > MAX_RADIX) {
      modulo = MAX_RADIX;
    }

    List<Character> buf = new ArrayList<>();

    while (i >= modulo) {
      buf.add(0, digits[(i % modulo)]);
      i = i / modulo;
    }
    buf.add(0, digits[i]);

    return Strings.createFromCharacters(buf).toString();

  }


  public static int toInt(String s) {
    return toInt(s, MAX_RADIX);
  }

  /**
   * @param s
   * @param radix
   * @return
   */
  public static int toInt(String s, int radix) {

    if (radix < MIN_RADIX) {
      throw new IllegalStateException("radix " + radix + " less than MIN_RADIX");
    }

    if (radix > MAX_RADIX) {
      throw new IllegalStateException("radix " + radix + " greater than MAX_RADIX");
    }

    int result = 0;
    int i = 0;
    Integer digitNumber;

    int len = s.length();
    while (i < len) {

      final char key = s.charAt(s.length()-1-i);
      digitNumber = digitsMap.get(key);
      if (digitNumber==null){
        throw new IllegalStateException("The generation of a string along a sequence is supported only with alphabetical characters. The character ("+key+") from the string ("+s+") is not alphabetic.");
      }

      int pow = Double.valueOf(Math.pow(radix, i)).intValue();
      result = result + digitNumber * pow;


      i++;

    }

    return result;
  }

  /**
   * The string is generated on the whole set of characters (a-z)
   *
   * @param i
   * @return
   */
  public static String toString(int i) {

    return toString(i, MAX_RADIX);

  }
}
