package com.tabulify.gen.generator;

import com.tabulify.gen.*;
import net.bytle.dag.Dag;
import net.bytle.dag.Dependency;

import java.util.Set;
import java.util.function.Supplier;

/**
 * A collection generator supports only the generation of one value.
 * <p>
 * Why ? Because they supports only one column:
 * * they can be generic
 * * they can supports functional generation {@link Supplier}
 * * a dag (Direct Acyclic Graph) can be build with a child-parent relationship
 * <p>
 * If a generator must generate multiple values,
 * it means that this is a parent generator and
 * therefore that its child must declare it via the {@link #getDependencies()}
 * <p>
 * For an example of child parent relationship, check the {@link ExpressionGenerator}
 *
 * @param <T> - the class that this generator supports
 */
public interface CollectionGenerator<T> extends Dependency, Supplier<T> {

  /**
   * Sqlite returns a default precision of 2000000000
   * which result in a maximum heap space exceptions
   */
  int MAX_STRING_PRECISION = 40000;
  /**
   * If there is no precision (on the column or on the type), this is the value/max given
   */
  int MAX_NUMBER_PRECISION = Integer.MAX_VALUE;


  static Dag<CollectionGenerator<?>> createDag() {
    return new Dag<>();
  }

  /**
   * How much data can this generator generate.
   * <p>
   * Example with a start of 0, a step of 1 and a maxValue of 2, the maxValue must be 2 (ie 1,2)
   *
   * @return the maxValue number of times the function getNewValue can be called, not capped by {@link GenDataPath#getMaxRecordCount()}
   */
  long getCount();

  /**
   * Used in a select stream to reset
   * the state of the generator to their first state
   */
  void reset();


  /**
   *
   * A utility function to get back to the data def
   * because a {@link DataGenerator} may have several columns
   *
   */
  GenRelationDef getRelationDef();

  /**
   * @return a new generated data object every time it's called for a single column generator
   * If the generator is multi-columns, it will throw an error
   * This is a alias for the functional function {@link Supplier#get()}
   */
  T getNewValue();

  /**
   * @return the actual value
   * (used in case of child-parent relationship between generator
   * <p>
   * Example: {@link ExpressionGenerator} accepts the value of others generators
   */
  T getActualValue();

  /**
   * @return the column attached to this generator
   * It permits to create parent relationship between generators
   * when asking a value for a column, we may need to ask the value for another column before
   * If the generator is a multi-column generator, it will throw an errors
   */
  GenColumnDef getColumnDef();


  /**
   * The dependent generator that must have been run before
   * this generator
   */
  Set<CollectionGenerator<?>> getDependencies();

  /**
   * @return the type of the generator
   * This value is used when casting from File to Class
   */
  DataGenType getGeneratorType();


  CollectionGenerator<?> setColumnDef(GenColumnDef genColumnDef);

}
