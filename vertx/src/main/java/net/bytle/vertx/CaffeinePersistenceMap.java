package net.bytle.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A caffeine persistent map
 * write through implementation
 * against the database
 */
public class CaffeinePersistenceMap <K,V> extends AbstractMap<K,V>
  implements Map<K,V> {


  private final Cache<K, V> cache;
  private final CaffeinePersistenceManager.MapConfig<K, V> mapConfig;


  public CaffeinePersistenceMap(CaffeinePersistenceManager.MapConfig<K, V> mapConfig) {

    this.mapConfig = mapConfig;
    cache = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.MINUTES)
      .maximumSize(1000)
      .removalListener((K key, V value, RemovalCause cause) -> {
        if (cause == RemovalCause.EXPLICIT) {
          System.out.println("Entry evicted (delete in store): " + key + "=" + value);
        }
      })
      .build();
  }




  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return cache.asMap().entrySet();
  }



}
