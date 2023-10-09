package net.bytle.db.flow.stream;

import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.spi.DataPath;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Supplier gives back a set of data path
 *
 * The set contains data path that have a relation
 *
 * Or says in other terms, set should be independent of each other.
 *
 * If two dependents data paths are separated in two set and if one
 * needs to be create before the other, the flow will get an error
 * if the target should be created
 *
 */
public interface DataPathSupplier extends Supplier<Set<DataPath>>, OperationStep, Iterator<Set<DataPath>> {


  /**
   * The number of set created
   * @return
   */
  int getSetCount();



}
