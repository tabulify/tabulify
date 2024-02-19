package net.bytle.vertx.collections;

import net.bytle.exception.CastException;
import org.jetbrains.annotations.NotNull;

/**
 * Every element V should have a serializer companion
 * to save its representation in the store
 * @param <V>
 */
public interface WriteThroughElementSerializer<V> {


  /**
   * @param element - the element
   * @return the id that is used to identify the element in the store
   */
  String getStoreId(@NotNull V element);

  /**
   *
   * @param element - the element
   * @return the element in a string representation to be stored in the store
   * @throws CastException - if any error
   */
  String serialize(@NotNull V element) throws CastException;

  /**
   * Return an element from the string value from the store
   * @param value - the string value
   * @return the element
   * @throws CastException - if any error
   */
  V deserialize(@NotNull String value) throws CastException;

  /**
   * When we retrieve the element from the database
   * we set the id back
   * Why? If the code of the id has changed, the id will still be consistent
   * @param element - the element
   * @param id - the id
   */
  void setStoreId(V element, String id);

}
