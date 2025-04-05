package net.bytle.db.flow.stream;

import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.spi.DataPath;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A supplier gives back a set of data path
 * <p>
 * The set contains data path that have a relation
 * <p>
 * Or says in other terms, set should be independent of each other.
 * <p>
 * If two dependents data paths are separated in two set and if one
 * needs to be created before the other, the flow will get an error
 * if the target should be created
 *
 */
public interface DataPathSupplier extends Supplier<Set<DataPath>>, OperationStep, Iterator<Set<DataPath>> {


  /**
   * @return The number of set created
   */
  int getSetCount();



}
