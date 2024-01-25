package net.bytle.vertx;

import com.github.benmanes.caffeine.cache.RemovalCause;

/**
 * A basic client that sends the operation to the console
 * for monitoring
 * @param <K> key
 * @param <V> value
 */
public class MapWriteThroughSinkConsole<K, V> implements MapWriteThroughSink<K, V> {

  @Override
  public void removalListener(K key, V value, RemovalCause removalCause) {

    /**
     * Explicit cause means that we explicitly deleted it
     */
    System.out.println(removalCause+" Remove Operation for " + key + "=" + value);

  }

  @Override
  public V get(K key) {
    System.out.println("Get Operation for the key " + key);
    return null;
  }

  @Override
  public V put(K key, V value) {
    System.out.println("Put Operation for the key " + key);
    return value;
  }


}
