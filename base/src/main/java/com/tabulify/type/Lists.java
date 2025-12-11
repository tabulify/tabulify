package com.tabulify.type;

import com.tabulify.exception.CastException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collections utility
 */
public class Lists {


    public static void reverse(List<?> list){
        Collections.reverse(list);
    }

  /**
   *
   * @param collection a collection
   * @param <T> the generic type of the collection
   * @return the duplicates in a set
   * @see <a href="https://stackoverflow.com/questions/7414667/identify-duplicates-in-a-list">Identify duplicates in a List</a>
   */
  public static <T> Set<T> findDuplicates(Collection<T> collection) {
    Set<T> uniques = new HashSet<>();
    return collection.stream()
      .filter(e -> !uniques.add(e))
      .collect(Collectors.toSet());
  }


  public static <T> List<T> minus(List<T> first, List<T> second) {
    first.removeAll(second);
    return first;
  }

  public static <T> List<T> castToNewList(List<?> list, Class<T> aClass) throws CastException {

    return Casts.castToNewList(list,aClass);

  }

  /**
   * Return a single hash of a list of string
   */
  public static String toHash(List<String> items) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Hash each item and combine the hash bytes
      items.forEach(item -> digest.update(item.getBytes()));

      byte[] finalHash = digest.digest();

      // bytes to hex
      return Bytes.toHexaDecimal(finalHash);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
