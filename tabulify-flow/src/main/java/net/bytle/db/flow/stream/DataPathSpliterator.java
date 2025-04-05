package net.bytle.db.flow.stream;

import net.bytle.db.spi.DataPath;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * An iterator for stream
 * that returns always the specified token (ie not infinite)
 */
public class DataPathSpliterator extends Spliterators.AbstractSpliterator<Set<DataPath>> {


  private final DataPathSupplier dataPathStreamSupplier;
  private int counter = 0;


  protected DataPathSpliterator(DataPathSupplier dataPathStreamSupplier) {
    super(dataPathStreamSupplier.getSetCount(), Spliterator.SIZED);
    this.dataPathStreamSupplier = dataPathStreamSupplier;
  }


  @Override
  public boolean tryAdvance(Consumer<? super Set<DataPath>> action) {
    if (counter > dataPathStreamSupplier.getSetCount() - 1) return false;
    action.accept(dataPathStreamSupplier.get());
    counter++;
    return true;
  }

}
