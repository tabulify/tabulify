package net.bytle.db.gen.generator;


import net.bytle.db.model.ColumnDef;

import java.util.List;

/**
 * A data generator generates a value for:
 * * one
 * * or several column.
 * of type T
 *
 * @param <T>
 */
public interface CollectionGenerator<T> {


  /**
   * Sqlite returns a default precision of 2000000000
   * which result in a maximum heap space exceptions
   */
  int MAX_STRING_PRECISION = 40000;

  /**
   * If there is no precision (on the column or on the type), this is the value/max given
   */
  int MAX_NUMBER_PRECISION = Integer.MAX_VALUE;



  /**
   * @return a new generated data object every time it's called for a single column generator
   * If the generator is multi-columns, it will throw an error
   */
  <T> T getNewValue();


  /**
   * @return a generated value (used in case of derived data for a single column generator
   * If the generator is multi-columns, it will throw an error
   */
  T getActualValue();

  /**
   * @return the column attached to this generator
   * It permits to create parent relationship between generators
   * when asking a value for a column, we may need to ask the value for another column before
   * If the generator is a multi-column generator, it will throw an errors
   */
  ColumnDef<T> getColumn();

  /**
   * of a new value for a column
   *
   * @return a new generated data object every time it's called
   */
  <T> T getNewValue(ColumnDef columnDef);

  /**
   * of the actual value of a column
   *
   * @return a generated value (used in case of derived data
   */
  T getActualValue(ColumnDef columnDef);

  /**
   * @return the columns attached to this generator
   */
  List<ColumnDef> getColumns();

  /**
   * How much data can this generator generate.
   * <p>
   * Example with a start of 0, a step 0f 1 and a maxValue of 2, the maxValue must be 2 (ie 1,2)
   *
   * @return the maxValue number of times the function {@link #getNewValue(ColumnDef)} can be called
   */
  Long getMaxGeneratedValues();


  /**
   *
   * @param <T>
   * @return the max generated value of the domain
   * If the collection generator generates values from 0 to 10, it will return 10
   */
  <T> T getDomainMax();

  /**
   *
   * @param <T>
   * @return the min generated value of the domain
   * If the collection generator generates values from 0 to 10, it will return 0
   */
  <T> T getDomainMin();
}
