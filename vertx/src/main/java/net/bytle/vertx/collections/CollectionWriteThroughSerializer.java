package net.bytle.vertx.collections;

import net.bytle.exception.CastException;
import org.jetbrains.annotations.NotNull;

/**
 * Every element V should have a serializer companion
 * to save its representation in the store
 * @param <V>
 */
public interface CollectionWriteThroughSerializer<V> {


  String getObjectId(@NotNull V value);

  String serialize(@NotNull V value) throws CastException;

  V deserialize(@NotNull String input) throws CastException;


}
