package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;

import java.util.Arrays;
import java.util.List;

/**
 * This is not really a generator has the data is in advance defined
 */
public class PredefinedDataGenerator<T> implements CollectionGeneratorOnce<T> {


  private final GenColumnDef<T> columnDef;
  private final Class<T> clazz;


  // The current value that the generated has generated
  private Object currentValue;


  /**
   * The sequence of value can be given and not derived
   * First element  = First element of the list
   * Second element = Second element of the list
   * ...
   */
  private List<Object> values;
  private Object actualValue;
  private int rowId = -1;


  /**
   * Create a data set generator from a specific predefined data
   *
   * @param columnDef
   * @param values    - The values to give bacl
   */
  public PredefinedDataGenerator(GenColumnDef<T> columnDef, Object... values) {

    this.columnDef = columnDef;
    this.clazz = columnDef.getClazz();
    this.values = Arrays.asList(values);

    // Do we have a property ?
    if (this.values.size() == 0) {
      final Object valuesAsObject = columnDef.getProperty("values");
      try {
        this.values = (List<Object>) valuesAsObject;
      } catch (ClassCastException e) {
        throw new RuntimeException("The values excepted for the column " + columnDef + " are not a list. The values are " + valuesAsObject);
      }
    }

  }

  /**
   * @return a new generated data object every time it's called
   * The next value is always the start value + the step
   * This is because when you insert data in a primary key column, you will give the maxValue and this generator
   * will give you the next value.
   */
  @Override
  public T getNewValue() {

    rowId++;
    this.actualValue = values.get(rowId);
    return clazz.cast(this.actualValue);


  }

  /**
   * @return a generated value (used in case of derived data
   */
  @Override
  public T getActualValue() {


    return clazz.cast(this.actualValue);

  }

  /**
   * @return the column attached to this generator
   */
  @Override
  public GenColumnDef<T> getColumn() {
    return columnDef;
  }


  @Override
  public String toString() {
    return "SequenceGenerator (" + columnDef + ")";
  }

  @Override
  public Long getMaxGeneratedValues() {
    return Long.valueOf(values.size());
  }
}
