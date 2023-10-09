package net.bytle.type;

import net.bytle.exception.CastException;

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

  public static <T> List<T> cast(List<?> list, Class<T> aClass) throws CastException {

    return Casts.castToList(list,aClass);

  }
}
