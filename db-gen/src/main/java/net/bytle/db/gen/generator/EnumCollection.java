package net.bytle.db.gen.generator;

import java.util.ArrayList;
import java.util.List;

public class EnumCollection<T> {


  private List<T> values = new ArrayList<>();


  public void addElement(T object) {
    values.add(object);
  }

  public int size() {
    return values.size();
  }

  public T getRandomValue() {
    int i = (int) (Math.random() * values.size());
    return values.get(i);
  }

}
