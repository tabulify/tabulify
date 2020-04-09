package net.bytle.db.gen.generator;

import java.util.function.Supplier;

public abstract class CollectionGeneratorOnceAbs<T> implements CollectionGeneratorOnce<T> {

  /**
   * The {@link java.util.function.Supplier supplier interface} has the returned function
   * called {@link Supplier#get()}.
   * An alias method
   * @return
   */
  @Override
  public T get() {
    return getNewValue();
  }

}
