package net.bytle.type;

import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;

import java.util.*;

/**
 * A map that normalize the key when looking up
 * with the {@link Key#toNormalizedKey(String)}
 * <p>
 * ie:
 * - case independent
 * - Space and separator independent
 *
 * @param <V>
 */
public class MapKeyIndependent<V> extends AbstractMap<String, V> implements Map<String, V> {

  Map<String, String> mapKey = new HashMap<>();

  Map<String, V> map = new HashMap<>();

  public static <V> MapKeyIndependent<V> createFrom(Map<?, ?> map, Class<V> classKey) {
    MapKeyIndependent<V> mapKey = new MapKeyIndependent<>();
    try {
      mapKey.putAll(Casts.castToSameMap(map, String.class, classKey));
    } catch (CastException e) {
      throw new InternalException("Should not throw as every object have a string method");
    }
    return mapKey;
  }


  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    String normalizedKey = Key.toNormalizedKey(key.toString());
    return mapKey.containsKey(normalizedKey);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    String normalizedKey = Key.toNormalizedKey(key.toString());
    String naturalKey = mapKey.get(normalizedKey);
    if (naturalKey == null) {
      return null;
    }
    return map.get(naturalKey);
  }

  @Override
  public V put(String key, V value) {
    String normalizedKey = Key.toNormalizedKey(key);
    String naturalKey = mapKey.get(normalizedKey);
    V oldValue = null;
    if (naturalKey != null) {
      oldValue = remove(key);
    }
    mapKey.put(normalizedKey, key);
    map.put(key, value);
    return oldValue;


  }

  @Override
  public V remove(Object key) {
    String normalizedKey = Key.toNormalizedKey(key.toString());
    String naturalKey = mapKey.remove(normalizedKey);
    return map.remove(naturalKey);

  }

  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    for (Map.Entry<? extends String, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    map.clear();
    mapKey.clear();
  }

  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Set<Entry<String, V>> entrySet() {
    return map.entrySet();
  }


  public <T> T get(Object key, Class<T> clazz) throws CastException {
    V v = get(key);
    if (v == null) {
      return null;
    } else {
      return Casts.cast(v, clazz);
    }
  }
}
