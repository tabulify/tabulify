package net.bytle.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Same idea as:
 * https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap
 * https://guava.dev/releases/snapshot/api/docs/com/google/common/collect/HashBiMap.html
 * @param <K>
 * @param <V>
 */
public class MapBiDirectional<K,V> implements Map<K, V> {

  Map<K, V> map = new HashMap<>();
  Map<V, K> inverseMap = new HashMap<>();

  public MapBiDirectional(Map<K, V> map) {
    this.map = map;
    for (Entry<K,V> entry:map.entrySet()){
      inverseMap.put(entry.getValue(),entry.getKey());
    }
  }

  public MapBiDirectional() {

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
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  /**
   *
   * @param value
   * @return the key
   */
  public K getKey(Object value) {
    return inverse().get(value);
  }

  /**
   *
   * @return the inverse
   */
  public MapBiDirectional<V, K> inverse() {
    return new MapBiDirectional<>(inverseMap);
  }

  @Override
  public V put(K key, V value) {
    K keyed = inverseMap.get(value);
    if (keyed ==null && key ==null){
      throw new RuntimeException("The value ("+value+") is already mapped to the null key");
    } else if (keyed!=null && !keyed.equals(key)){
      throw new RuntimeException("The value ("+value+") is already mapped to the key ("+keyed+"). You can't add it to the key ("+key+")");
    } else {
      inverseMap.put(value,key);
      return map.put(key, value);

    }
  }

  @Override
  public V remove(Object key) {
    V remove = map.remove(key);
    return remove;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }


  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }


  @Override
  public String toString() {
    return  map.toString();
  }

}
