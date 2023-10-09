package net.bytle.type;

import java.util.*;

/**
 * The string representation of an object that was made
 * {@link Key#toNormalizedKey(String)}
 */
public class SetKeyIndependent<E> extends AbstractSet<E> implements Set<E> {


  private final MapKeyIndependent<E> hashMap = new MapKeyIndependent<>();


  @Override
  public Iterator<E> iterator() {
    return hashMap.values().iterator();
  }

  @Override
  public int size() {
    return hashMap.size();
  }

  @Override
  public boolean add(E o) {
    E oldElement = hashMap.put(o.toString(), o);
    return oldElement == null || !oldElement.equals(o);
  }

  @Override
  public boolean removeAll(Collection c) {
    if (hashMap.size() == 0) {
      return false;
    }
    for (Map.Entry<String, E> entry : this.hashMap.entrySet()) {
      this.hashMap.remove(entry.getKey());
    }
    return true;
  }

  @Override
  public boolean contains(Object o) {
    return this.hashMap.containsKey(o);
  }
}
