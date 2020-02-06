package net.bytle.type;

import java.util.Arrays;

/**
 * Arrays utilities
 * Two ss to not clash with java.utils.Arrays
 */
public class Arrayss {

  /**
   * Does the array contains a character
   *
   * @param haystack
   * @param needle
   * @return
   */
  public static boolean in(char[] haystack, char needle) {
    for (char c : haystack) {
      if (needle == c) {
        return true;
      }
    }
    return false;
  }


  /**
   * Concat bytes array
   *
   * @param first
   * @param rest
   * @return
   */
  public static byte[] concatAll(byte[] first, byte[]... rest) {
    int totalLength = first.length;
    for (byte[] array : rest) {
      totalLength += array.length;
    }
    byte[] result = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (byte[] array : rest) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  /**
   * StackOverflow
   * https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
   *
   * @param first
   * @param rest
   * @param <T>
   * @return
   */
  public static <T> T[] concatAll(T[] first, T[]... rest) {
    int totalLength = first.length;
    for (T[] array : rest) {
      totalLength += array.length;
    }
    T[] result = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (T[] array : rest) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  /**
   *
   * @param first
   * @param length
   * @param <T>
   * @return the same array but with an new length
   */
  public static <T> T[] extend(T[] first, int length) {
    return Arrays.copyOf(first, length);
  }

  /**
   * StackOverflow
   *
   * @param first
   * @param elements
   * @param <T>
   * @return
   */
  public static <T> T[] concat(T first, T[] elements) {
    int totalLength = elements.length + 1;
    T[] result = Arrays.copyOf(elements, totalLength);
    result[0]=first;
    System.arraycopy(elements, 0, result, 1, totalLength-1);
    return result;
  }

  /**
   * Print an array of string
   *
   * @param strings
   */
  public static void print(String[] strings) {
    System.out.println(String.join(",", strings));
  }
}
