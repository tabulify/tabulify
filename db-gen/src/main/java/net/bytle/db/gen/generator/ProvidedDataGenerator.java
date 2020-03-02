package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;

import java.util.Arrays;
import java.util.List;

/**
 * The data of this generator is provided of length N
 * and it will generate (ie give back) one record
 * at a time with a maximum of N
 */
public class ProvidedDataGenerator<T> implements CollectionGeneratorOnce<T> {


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
  public ProvidedDataGenerator(GenColumnDef<T> columnDef, Object... values) {

    this.columnDef = columnDef;
    this.clazz = columnDef.getClazz();

    // If the first value is a list
    if (values.length==1){
      if (values[0] instanceof List){
        this.values = (List<Object>) values[0];
      }
    } else {
      this.values = Arrays.asList(values);
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
    return "Provided Data Generator (" + columnDef + ")";
  }

  @Override
  public Long getMaxGeneratedValues() {
    return (long) values.size();
  }

}
