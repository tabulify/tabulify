package com.tabulify.type;

import com.tabulify.exception.CastException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.*;

public class CastsTest {


  @Test
  public void castIntegerToStringTest() throws CastException {
    Integer source = 25;
    Assertions.assertEquals("25", Casts.cast(source, String.class), "Same data");
  }

  @Test
  public void castStringBigintTest() throws CastException {
    String source = "0";
    BigInteger target = new BigInteger("0");
    Assertions.assertEquals(target, Casts.cast(source, BigInteger.class), "Same data");
  }

  @Test
  public void castSqlTimeTest() throws CastException {

    Time expected = Time.valueOf("08:00:00");
    Assertions.assertEquals(expected, Casts.cast("08:00", java.sql.Time.class), "Same data");
    Assertions.assertEquals(expected, Casts.cast("08:00:00.000", java.sql.Time.class), "Same data");

  }

  @SuppressWarnings("unchecked")
  @Test
  void collectionCastTest() throws CastException {

    HashSet<Integer> source = new HashSet<>();
    source.add(1);
    source.add(2);
    HashSet<Integer> cast = Casts.cast(source, HashSet.class, Integer.class);
    Assertions.assertEquals(source, cast, "Same data");


    HashSet<String> castAsString = Casts.cast(source, HashSet.class, String.class);
    HashSet<String> expectedStringSet = new HashSet<>();
    expectedStringSet.add("1");
    expectedStringSet.add("2");
    Assertions.assertEquals(expectedStringSet, castAsString, "Class match");

    /**
     * Integer is not a collection
     */
    CastException exception = Assertions.assertThrows(
      CastException.class,
      () -> Casts.cast(source, Integer.class, String.class)
    );
    Assertions.assertTrue(exception.getMessage().contains("not a collection"));

    /**
     * List.class is an interface and cannot be instantiated
     */
    exception = Assertions.assertThrows(
      CastException.class,
      () -> Casts.cast(source, List.class, String.class)
    );
    Assertions.assertTrue(exception.getMessage().contains("interface"));

    /**
     * From HashSet to List
     */
    List<String> castAsListString = Casts.cast(source, ArrayList.class, String.class);
    List<String> expectedListString = new ArrayList<>();
    expectedListString.add("1");
    expectedListString.add("2");
    Assertions.assertEquals(expectedListString, castAsListString, "List class match");

    /**
     * Null
     */
    List<String> castNull = Casts.cast(null, ArrayList.class, String.class);
    Assertions.assertNull(castNull, "Null is null");


  }

  @Test
  public void castListToArray() throws CastException {
    List<Object> lists = Arrays.asList("1", "2", "3");
    Integer[] listsArray = Casts.castToArray(lists, Integer.class);
    Assertions.assertEquals(listsArray.length, lists.size());
    Assertions.assertEquals((Integer) 1, listsArray[0]);
    Assertions.assertEquals((Integer) 2, listsArray[1]);
    Assertions.assertEquals((Integer) 3, listsArray[2]);
  }

  @Test
  public void castObjectToArray() throws CastException {
    Object[] lists = {"1", "2", "3"};
    String[] listsArray = Casts.cast(lists, String[].class);
    Assertions.assertEquals(listsArray.length, lists.length);
    Assertions.assertEquals("1", listsArray[0]);
    Assertions.assertEquals("2", listsArray[1]);
    Assertions.assertEquals("3", listsArray[2]);
  }

  @Test
  public void castToNewListFromArrayTest() throws CastException {
    Object[] arrays = {"1", "2", "3"};
    List<Integer> list = Casts.castToNewList(arrays, Integer.class);
    Assertions.assertEquals(arrays.length, list.size());
    Assertions.assertEquals(Integer.valueOf(arrays[0].toString()), list.get(0));
    Assertions.assertEquals(Integer.valueOf(arrays[1].toString()), list.get(1));
    Assertions.assertEquals(Integer.valueOf(arrays[2].toString()), list.get(2));
  }

  @Test
  public void castToNewListFromSetTest() throws CastException {
    Object[] arrays = {"1", "2", "3"};
    Set<Object> set = new HashSet<>(Arrays.asList(arrays));
    List<Integer> list = Casts.castToNewList(set, Integer.class);
    Assertions.assertEquals(arrays.length, list.size());
    Assertions.assertEquals(Integer.valueOf(arrays[0].toString()), list.get(0));
    Assertions.assertEquals(Integer.valueOf(arrays[1].toString()), list.get(1));
    Assertions.assertEquals(Integer.valueOf(arrays[2].toString()), list.get(2));
  }

  @Test
  public void castToPathTest() throws CastException {
    Path path = Paths.get(".").toAbsolutePath();
    String stringPath = path.toString();
    Path castPath = Casts.cast(stringPath, Path.class);
    Assertions.assertEquals(path, castPath);
  }

  @Test
  public void castToEnumTest() throws CastException {

    CastsEnum castEnum = Casts.cast("test", CastsEnum.class);
    Assertions.assertEquals(CastsEnum.TEST, castEnum);

    CastsEnum castEnumFromValueOf;
    try {
      CastsEnum.valueOf("test");
    } catch (IllegalArgumentException e) {
      // throws because it does not match exactly
    }
    castEnumFromValueOf = CastsEnum.valueOf("TEST");
    Assertions.assertEquals(CastsEnum.TEST, castEnumFromValueOf);

  }

  @SuppressWarnings({"ConstantConditions", "CastCanBeRemovedNarrowingVariableType"})
  @Test
  public void castsNull() {

    Object o = null;
    String s = (String) o;
    Assertions.assertNull(s);

  }

  @Test
  void isNarrowingConversion() {

    Assertions.assertFalse(Casts.isNarrowingConversion(java.sql.Date.class,java.sql.Timestamp.class));

  }
}
