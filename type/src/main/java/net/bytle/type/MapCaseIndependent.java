package net.bytle.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapCaseIndependent<V> implements Map<String, V> {

  Map<String, V> map = new HashMap<>();


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
    for (Map.Entry<String, V> entry : map.entrySet()) {
      if (((String) key).toLowerCase().equals(entry.getKey().toLowerCase())) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public V put(String key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
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

  public Long getAsLong(V key) {
    Object object = get(key);
    return (Long) object;
  }
}
