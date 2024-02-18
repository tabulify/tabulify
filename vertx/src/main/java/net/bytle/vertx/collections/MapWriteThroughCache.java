package net.bytle.vertx.collections;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * A caffeine persistent map
 * write through implementation
 * against the database
 */
public class MapWriteThroughCache<K,V>  {


  private final Cache<K, V> cache;
  private final MapWriteThroughCacheBuilder.MapConfig<K, V> mapConfig;


  public MapWriteThroughCache(MapWriteThroughCacheBuilder.MapConfig<K, V> mapConfig) {

    this.mapConfig = mapConfig;
    cache = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.MINUTES)
      .maximumSize(1000)
      .removalListener(mapConfig.sink::removalListener)
      .build();
  }

  public V put(K key, V value) {
    mapConfig.sink.put(key,value);
    cache.put(key,value);
    return value;
  }

  public V get(Object key) {
    //noinspection unchecked
    return cache.get((K) key,mapConfig.sink::get);
  }

  /**
   * @param key key whose mapping is to be removed from the map
   */
  public void remove(Object key) {
    // https://github.com/ben-manes/caffeine/wiki/Removal
    //noinspection unchecked
    cache.invalidate((K) key);
  }





}
