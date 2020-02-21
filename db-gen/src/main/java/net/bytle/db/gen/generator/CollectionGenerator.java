package net.bytle.db.gen.generator;

public interface CollectionGenerator<T> {

  /**
   * How much data can this generator generate.
   * <p>
   * Example with a start of 0, a step 0f 1 and a maxValue of 2, the maxValue must be 2 (ie 1,2)
   *
   * @return the maxValue number of times the function getNewValue can be called
   */
  Long getMaxGeneratedValues();



}
