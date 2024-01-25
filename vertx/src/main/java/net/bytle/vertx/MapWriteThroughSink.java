package net.bytle.vertx;

import com.github.benmanes.caffeine.cache.RemovalCause;

public interface MapWriteThroughSink<K,V> {

  void removalListener(K key, V value, RemovalCause removalCause);


  V get(K key);

  V put(K key, V value);

}
