package com.tabulify.gen.generator;


import net.bytle.type.Strings;

import java.util.*;

/**
 * A class that build a string
 * from an int to a string and back
 */
public class StringGenerator {

  final static Character[] lowerCaseAlpha = {
    'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h',
    'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z',
  };

  final static Character[] digits = {
    '1', '2',
    '3', '4', '5', '6', '7', '8',
    '9'
  };


  final Map<Character, Integer> charactersMap = new HashMap<>();

  private final ArrayList<Character> characters;

  /**
   * The radix is the total number of characters
   * Decimals are for 10 digits from 0 to 9
   */
  private final int MAX_RADIX;
  private static final int MIN_RADIX = 2;


  public StringGenerator(SequenceGeneratorStringBuilder builder) {
    /**
     * The set of possible characters
     */
    characters = new ArrayList<>(Arrays.asList(lowerCaseAlpha));
    if (builder.caseSensitivity) {
      for (Character c : lowerCaseAlpha) {
        characters.add(Character.toUpperCase(c));
      }
    }
    if (builder.withDigits) {
      characters.addAll(Arrays.asList(digits));
    }

    MAX_RADIX = characters.size();
    for (int i = 0; i < characters.size(); i++) {
      charactersMap.put(characters.get(i), i);
    }
  }

  public static SequenceGeneratorStringBuilder builder() {
    return new SequenceGeneratorStringBuilder();
  }

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
  public String toString(long i, int modulo) {

    if (modulo < MIN_RADIX || modulo > MAX_RADIX) {
      modulo = MAX_RADIX;
    }

    List<Character> buf = new ArrayList<>();

    while (i >= modulo) {
      buf.add(0, characters.get((int) (i % modulo)));
      i = i / modulo;
    }
    buf.add(0, characters.get((int) i));

    return Strings.createFromCharacters(buf).toString();

  }


  public long toInt(String s) {
    return toInt(s, MAX_RADIX);
  }


  public long toInt(String s, int radix) {

    if (radix < MIN_RADIX) {
      throw new IllegalStateException("radix " + radix + " less than MIN_RADIX");
    }

    if (radix > MAX_RADIX) {
      throw new IllegalStateException("radix " + radix + " greater than MAX_RADIX");
    }

    long result = 0;
    int i = 0;
    Integer digitNumber;

    int len = s.length();
    while (i < len) {

      final char key = s.charAt(s.length()-1-i);
      digitNumber = charactersMap.get(key);
      if (digitNumber==null){
        throw new IllegalStateException("The generation of a string along a sequence is supported only with alphabetical characters. The character ("+key+") from the string ("+s+") is not alphabetic.");
      }
      long pow = (long) Math.pow(radix, i);
      result = result + digitNumber * pow;


      i++;

    }

    return result;
  }

  /**
   * The string is generated on the whole set of characters (a-z)
   */
  public String toString(long i) {

    return toString(i, MAX_RADIX);

  }

  public int getMaxRadix() {
    return MAX_RADIX;
  }

  public static class SequenceGeneratorStringBuilder {
    private boolean caseSensitivity = true;
    private boolean withDigits = true;

    public SequenceGeneratorStringBuilder setCaseSensitivity(boolean b) {
      this.caseSensitivity = b;
      return this;
    }

    public SequenceGeneratorStringBuilder setWithDigits(boolean b) {
      this.withDigits = b;
      return this;
    }

    public StringGenerator build() {
      return new StringGenerator(this);
    }
  }
}
