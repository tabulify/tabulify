package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;

/**
 * A data generator generates a value for:
 * * one
 * * or several column.
 * of type T
 *
 * @param <T>
 */
public interface CollectionGeneratorOnce<T> extends CollectionGenerator {


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
  GenColumnDef<T> getColumn();





}
