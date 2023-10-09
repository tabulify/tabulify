package net.bytle.type;

import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.type.time.Date;
import net.bytle.type.time.Time;
import net.bytle.type.time.Timestamp;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


public class Casts {

  protected static Set<String> nullableStrings = new HashSet<>(Arrays.asList("", "null", "na"));


  /**
   * @param sourceObject - the object to cast
   * @param targetClass  - the class to cast
   * @param <T>          - the receiver class
   * @return null if the object is null, throw an exception if the class is not the expected one
   * the object to the asked clazz
   * @throws CastException when the cast does not work
   */
  public static <T> T cast(Object sourceObject, Class<T> targetClass) throws CastException {

    /**
     * Null
     */
    if (sourceObject == null) {
      return null;
    }

    try {

      Class<?> sourceObjectClass = sourceObject.getClass();

      /**
       * Same class
       */
      if (sourceObjectClass.equals(targetClass)) {
        return targetClass.cast(sourceObject);
      }

      if (sourceObjectClass.isArray()) {
        if (!targetClass.isArray()) {
          if(targetClass.equals(String.class)){
             String[] values = castToArray(sourceObject, String.class);
            //noinspection unchecked
            return (T) String.join(", ", values);
          }
          throw new CastException("The source object is an array and the target class is not");
        }
        //noinspection unchecked
        return (T) castToArray(sourceObject, targetClass.getComponentType());
      }

      /**
       * Nullable string
       */
      if (targetClass != String.class && sourceObjectClass == String.class) {
        if (nullableStrings.contains(sourceObject)) {
          return null;
        }
      }

      /**
       * Long
       */
      if (targetClass == Long.class) {
        return targetClass.cast(Longs.createFromObject(sourceObject).toLong());
      }

      /**
       * Boolean
       */
      if (targetClass == Boolean.class) {
        return targetClass.cast(Booleans.createFromObject(sourceObject).toBoolean());
      }
      /**
       * Integer and Smallint
       */
      if (targetClass == Integer.class) {
        return targetClass.cast(Integers.createFromObject(sourceObject).toInteger());
      }

      /**
       * Big integer
       */
      if (targetClass == BigInteger.class) {
        return targetClass.cast(BigIntegers.createFromObject(sourceObject).toBigInteger());
      }

      /**
       * Big Decimal (exact number), Numeric, decimal
       */
      if (targetClass == BigDecimal.class) {
        return targetClass.cast(BigDecimals.createFromObject(sourceObject).toBigDecimal());
      }

      /**
       * Float Double precision
       */
      if (targetClass == Double.class) {
        return targetClass.cast(Doubles.createFromObject(sourceObject).toDouble());
      }

      /**
       * Float Single precision
       */
      if (targetClass == Float.class) {
        /**
         * Not really error proof against precision error but yeah
         * Float is no more used
         */
        return targetClass.cast(Doubles.createFromObject(sourceObject).toFloat());
      }

      /**
       * Date
       */
      if (targetClass == java.sql.Date.class) {
        return targetClass.cast(Date.createFromObject(sourceObject).toSqlDate());
      }
      if (targetClass == LocalDate.class) {
        return targetClass.cast(Date.createFromObject(sourceObject).toLocalDate());
      }

      /**
       * Timestamp
       */
      if (targetClass == java.sql.Timestamp.class) {
        return targetClass.cast(Timestamp.createFromObject(sourceObject).toSqlTimestamp());
      }
      if (targetClass == LocalDateTime.class) {
        return targetClass.cast(Timestamp.createFromObject(sourceObject).toLocalDateTime());
      }
      if (targetClass == java.util.Date.class) {
        return targetClass.cast(Date.createFromObject(sourceObject).toDate());
      }


      /**
       * String
       */
      if (targetClass == String.class) {
        return targetClass.cast(sourceObject.toString());
      }

      /**
       * Time
       */
      if (targetClass == java.sql.Time.class) {
        return targetClass.cast(Time.createFromObject(sourceObject).toSqlTime());
      }

      /**
       * Time
       */
      if (targetClass == java.sql.SQLXML.class) {
        return targetClass.cast(SqlXmlFromString.create(sourceObject.toString()));
      }

      /**
       * Clob
       */
      if (targetClass == java.sql.Clob.class) {
        return targetClass.cast(SqlClob.createFromObject(sourceObject));
      }

      /**
       * Path from string
       */
      if (targetClass == java.nio.file.Path.class) {
        return targetClass.cast(Paths.get(sourceObject.toString()));
      }

      /**
       * Enum
       */
      if (targetClass.isEnum()) {
        /**
         * {@link Enums#valueOf(Class, String)} is not used
         * because it needs exact match
         */
        String normalizedLookupKey = Key.toNormalizedKey(sourceObject.toString());
        for (T constant : targetClass.getEnumConstants()) {
          if (constant == null) {
            throw new InternalError("The enum class (" + targetClass + ") does not have any constants");
          }
          String normalizedConstantKey = Key.toNormalizedKey(((Enum<?>) constant).name());
          if (normalizedConstantKey.equals(normalizedLookupKey)) {
            return constant;
          }
        }
      }

      /**
       * Charset
       */
      if (targetClass == Charset.class) {
        String charsetValue = sourceObject.toString();
        if (!Charset.isSupported(charsetValue)) {
          throw new IllegalCharsetNameException("The character set value (" + charsetValue + ") is not supported. You may set the character set to one of this values: " + String.join(", ", Charset.availableCharsets().keySet()));
        }
        return targetClass.cast(Charset.forName(charsetValue));

      }

      /**
       * If we are here, we have not yet a
       * transformation,
       * we try to cast it directly
       */
      try {
        return targetClass.cast(sourceObject);
      } catch (ClassCastException e) {
        throw new ClassCastException("We couldn't cast the value (" + sourceObject + ") with the class (" + sourceObjectClass.getSimpleName() + ") to the class (" + targetClass.getSimpleName() + ")");
      }

    } catch (IllegalCharsetNameException | ClassCastException e) {
      throw new CastException(e.getMessage(), e);
    }

  }

  public static <T> T[] castToArray(List<T> list) {

    //noinspection unchecked
    return (T[]) list.toArray();

  }

  /**
   * @param object      - a sequence of single values
   * @param targetClazz - the class to cast
   * @param <T>         - the type of class to return
   * @return A mix of Source: Effective Java; Item 26
   * <p>
   * (E[])new Object[INITIAL_ARRAY_LENGTH]
   * <p>
   * and
   * <p>
   * {@link Array#newInstance(Class, int)}
   */
  public static <T> T[] castToArray(Object object, Class<T> targetClazz) throws CastException {

    Class<?> valueObjectClass = object.getClass();

    if (targetClazz.isArray()) {
      throw new CastException("The target clazz should not be an array");
    }

    if (valueObjectClass.isArray()) {
      if (valueObjectClass.getComponentType() == targetClazz) {
        //noinspection unchecked
        return (T[]) object;
      }
      List<T> target = new ArrayList<>();
      for (Object value : (Object[]) object) {
        target.add(Casts.cast(value, targetClazz));
      }
      //noinspection unchecked
      return target.toArray((T[]) Array.newInstance(targetClazz, target.size()));
    }

    if (object instanceof Collection) {
      /**
       * The cast of the generic throw an unchecked
       * warning that is not true
       */
      List<T> target = new ArrayList<>();
      for (Object value : (Collection<?>) object) {
        target.add(Casts.cast(value, targetClazz));
      }
      //noinspection unchecked
      return target.toArray((T[]) Array.newInstance(targetClazz, target.size()));
    }

    throw new CastException("We could cast the value to an array of (" + targetClazz + ")");
  }

  /**
   * Cast a map to another one b y creating a new map
   *
   * @param object - the object to cast
   * @param clazzK - the key class
   * @param clazzV - the value class
   * @param <K>    - the type of key
   * @param <V>    - the type of value
   * @return the map
   */
  public static <K, V> Map<K, V> castToNewMap(Object object, Class<K> clazzK, Class<V> clazzV) throws CastException {

    Map<?, ?> map;
    if (!(object instanceof Map)) {
      throw new ClassCastException("The object is not a map but a " + object.getClass().getSimpleName() + " and can't be then casted");
    } else {
      map = (Map<?, ?>) object;
    }

    Map<K, V> result = new HashMap<>();
    for (Map.Entry<?, ?> e : map.entrySet()) {
      result.put(
        Casts.cast(e.getKey(), clazzK),
        Casts.cast(e.getValue(), clazzV)
      );
    }

    return result;

  }

  /**
   * This function cast the object to a map and don't create new one (meaning that the object
   * are not cast)
   *
   * @param object - an object
   * @param clazzK - the key class
   * @param clazzV - the value class
   * @param <K>    the key type
   * @param <V>    the value type
   * @return the same object but casted
   * @throws CastException if there is a problem
   */
  public static <K, V> Map<K, V> castToSameMap(Object object, Class<K> clazzK, Class<V> clazzV) throws CastException {

    Map<?, ?> map;
    if (!(object instanceof Map)) {
      throw new CastException("The object (value: " + object + ") is not a map but a " + object.getClass().getSimpleName() + " and can't be then casted");
    } else {
      map = (Map<?, ?>) object;
    }

    for (Map.Entry<?, ?> e : map.entrySet()) {
      if (e.getKey() != null && !clazzK.equals(Object.class)) {
        if (!e.getKey().getClass().equals(clazzK)) {
          throw new CastException("The key (" + e.getKey() + ") is not a " + clazzK.getSimpleName() + ".");
        }
      }
      if (e.getValue() != null && !clazzV.equals(Object.class)) {
        if (!e.getValue().getClass().equals(clazzV)) {
          throw new CastException("The key (" + e.getValue() + ") is not a " + clazzV.getSimpleName() + ".");
        }
      }
    }

    //noinspection unchecked
    return (Map<K, V>) object;

  }

  /**
   * Cast a list of unknown class to a list of clazz.
   *
   * @param clazz - the target class to cast
   * @param o     - a {@link Collection collection (list,array)} or an array
   * @param <T>   the return type
   * @return the list
   */
  public static <T> List<T> castToList(Object o, Class<T> clazz) throws CastException {

    if (o == null) {
      return null;
    }


    if (o instanceof List) {
      List<?> list = (List<?>) o;
      List<T> returnList = new ArrayList<>();
      for (Object object : list) {
        if (object.getClass() == clazz) {
          //noinspection unchecked
          return (List<T>) list;
        }
        returnList.add(cast(object, clazz));
      }
      return returnList;
    }

    if (o instanceof Collection) {
      Collection<?> array = ((Collection<?>) o);
      List<T> returnList = new ArrayList<>();
      for (Object object : array) {
        returnList.add(cast(object, clazz));
      }
      return returnList;
    }

    if (o.getClass().isArray()) {
      Object[] array = (Object[]) o;
      List<T> returnList = new ArrayList<>();
      for (Object object : array) {
        returnList.add(cast(object, clazz));
      }
      return returnList;
    }

    throw new IllegalArgumentException("The object is not a collection (list, set) nor an array but a " + o.getClass().getSimpleName() + " and can't therefore be cast to a list");

  }

  @SuppressWarnings("unused")
  public static <T> Set<T> toSameSet(Object object, Class<T> clazzV) {

    Set<?> set;
    if (!(object instanceof Set)) {
      throw new ClassCastException("The object (value: " + object + ") is not a set but a " + object.getClass().getSimpleName() + " and can't be then casted");
    } else {
      set = (Set<?>) object;
    }

    for (Object value : set) {
      if (!clazzV.equals(Object.class)) {
        if (!clazzV.isAssignableFrom(value.getClass())) {
          throw new ClassCastException("The value (" + value + ") is not a " + clazzV.getSimpleName() + ".");
        }
      }
    }

    //noinspection unchecked
    return (Set<T>) object;

  }

  /**
   * A cast that throws errors only at runtime
   *
   * @param value  the value
   * @param aClass the class
   * @param <T>    the type
   * @return the cast object
   */
  public static <T> T castSafe(Object value, Class<T> aClass) {
    try {
      return cast(value, aClass);
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> List<T> castToListSafe(Object o, Class<T> aClass) {
    try {
      return castToList(o, aClass);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromValue(o, e);
    }
  }

  public static <K, V> Map<K, V> castToNewMapSafe(Object o, Class<K> kClass, Class<V> vClass) {
    try {
      return castToNewMap(o, kClass, vClass);
    } catch (CastException e) {
      throw new ClassCastException(e.getMessage());
    }
  }
}
