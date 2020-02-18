package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.model.ColumnDef;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Generate in sequence data over:
 * * the integer domain
 * * the date domain
 * * or a list of data domain
 * <p>
 * <p>
 * Each time, the {@link #getNewValue()} function is called
 * * For integer and date, it will calculate the new value from the start value plus or minValue the step
 * * For a list domain, it will take the first index defined by start and of the next one defined by step.
 * <p>
 * <p>
 * * start (may be a date, a number) - default to 0 / current date
 * * step  integer (number or number of day) - default to +1
 * * maxValue  only active if the step is positive
 * * minValue  only active if the step is negative
 * * values in case of a list of data (may be null)
 */

public class SequenceCollectionGenerator<T> implements CollectionGenerator<T> {


  private final ColumnDef<T> columnDef;
  private final Class<T> clazz;


  // The current value that the generated has generated
  private Object currentValue;

  // The min value is the lower limit value
  private Object minValue;
  // The max value is the upper limit value
  private Object maxValue;
  // The step is the number of step in the sequence (Generally one)
  private int step;

  /**
   * The sequence of value can be given and not derived
   * First element  = First element of the list
   * Second element = Second element of the list
   * ...
   */
  private List<Object> values;


  /**
   * @param columnDef
   */
  public SequenceCollectionGenerator(ColumnDef<T> columnDef) {

    this.columnDef = columnDef;
    this.clazz = columnDef.getClazz();

    if (clazz == Integer.class) {
      currentValue = 0;
      maxValue = Integer.MAX_VALUE;
      step = 1;
    } else if (clazz == Double.class) {
      currentValue = 0.0;
      maxValue = Double.MAX_VALUE;
      step = 1;
    } else if (clazz == Date.class) {
      currentValue = LocalDate.now();
      step = -1;
      minValue = LocalDate.MIN;
      maxValue = LocalDate.MAX;
    } else if (clazz == BigDecimal.class) {
      currentValue = new BigDecimal(0);
      step = 1;
      minValue = Integer.MIN_VALUE;
      maxValue = Integer.MAX_VALUE;
    } else if (clazz == String.class) {
      currentValue = -1;
      step = 1;
      minValue = 0; // The first code point
      // See the test SeqGenTest.sequenceGeneratorStringCharacterPrintTest to print them
    } else {
      throw new UnsupportedOperationException("The class (" + clazz + ") is not yet implemented");
    }


  }

  /**
   * The properties of a generator from a data definition file
   *
   * @param columnDef
   * @param <T>
   * @return
   */
  public static <T> SequenceCollectionGenerator<T> of(GenColumnDef<T> columnDef) {

    SequenceCollectionGenerator<T> sequenceGenerator = new SequenceCollectionGenerator<>(columnDef);


    final Object stepObj = columnDef.getProperty("step");
    final Integer step;
    try {
      step = (Integer) stepObj;
    } catch (ClassCastException e) {
      throw new RuntimeException("The step property for the data generator of the column (" + columnDef.getFullyQualifiedName() + ") is not an integer (Value: " + stepObj + ")");
    }
    if (step != null) {
      sequenceGenerator.step = step;
    }

    List<Object> values;
    final Object valuesAsObject = columnDef.getProperty("values");
    try {
      values = (List<Object>) valuesAsObject;
    } catch (ClassCastException e) {
      throw new RuntimeException("The values excepted for the column " + columnDef + " are not a list. The values are " + valuesAsObject);
    }
    if (values != null) {
      sequenceGenerator.values = values;
    }

    return sequenceGenerator;
  }

  /**
   * @return a new generated data object every time it's called
   * The next value is always the start value + the step
   * This is because when you insert data in a primary key column, you will give the maxValue and this generator
   * will give you the next value.
   */
  @Override
  public T getNewValue() {

    Object returnValue = null;

    if (clazz == Integer.class) {
      currentValue = (Integer) (currentValue) + step;
      returnValue = currentValue;
    }
    if (clazz == Double.class) {
      currentValue = (Double) (currentValue) + step;
      returnValue = currentValue;
    }
    if (clazz == Date.class) {
      currentValue = ((LocalDate) currentValue).plusDays(step);
      returnValue = Date.valueOf((LocalDate) currentValue);
    }
    if (clazz == BigDecimal.class) {
      currentValue = ((BigDecimal) (currentValue)).add(new BigDecimal(step));
      returnValue = currentValue;
    }
    if (clazz == String.class) {
      currentValue = (Integer) (currentValue) + step;
      if (values != null) {
        if ((Integer) currentValue > values.size()) {
          throw new RuntimeException("You cannot ask more than " + values.size() + " times a new value for this generator because its values are defined with a list of " + values.size() + " elements");
        }
        returnValue = values.get((Integer) currentValue);
      } else {
        Integer precisionOrMax = columnDef.getPrecisionOrMax();
        ;
        Integer precision = precisionOrMax != null ? precisionOrMax : MAX_STRING_PRECISION;
        returnValue = StringGenerator.toString((Integer) currentValue, StringGenerator.MAX_RADIX, precision);
      }
    }

    return clazz.cast(returnValue);


  }

  /**
   * @return a generated value (used in case of derived data
   */
  @Override
  public T getActualValue() {

    Object returnValue = currentValue;
    if (clazz == Date.class) {
      returnValue = Date.valueOf((LocalDate) currentValue);
    }
    if (clazz == String.class) {
      returnValue = StringGenerator.toString((Integer) currentValue, StringGenerator.MAX_RADIX, columnDef.getPrecision());
    }
    return clazz.cast(returnValue);

  }

  /**
   * @return the column attached to this generator
   */
  @Override
  public ColumnDef<T> getColumn() {
    return columnDef;
  }

  /**
   * of a new value for a column
   *
   * @param columnDef
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue(ColumnDef columnDef) {
    if (columnDef.equals(this.columnDef)) {
      return getNewValue();
    } else {
      throw new RuntimeException("Multiple column generator is not implemented");
    }
  }

  /**
   * of the actual value of a column
   *
   * @param columnDef
   * @return a generated value (used in case of derived data
   */
  @Override
  public T getActualValue(ColumnDef columnDef) {
    if (columnDef.equals(this.columnDef)) {
      return getActualValue();
    } else {
      throw new RuntimeException("Multiple column generator is not implemented");
    }
  }

  /**
   * @return the columns attached to this generator
   */
  @Override
  public List<ColumnDef> getColumns() {
    List<ColumnDef> columnDefs = new ArrayList<>();
    columnDefs.add(columnDef);
    return columnDefs;
  }

  /**
   * @param start is the start value
   * @return
   */
  public SequenceCollectionGenerator<T> start(T start) {

    // When checking the minValue date in a table, the returned value may be null
    // for the sake of simplicity we are not throwing an error
    if (start != null) {

      if (start.getClass() != clazz) {

        if (clazz == BigDecimal.class && start.getClass() == Integer.class) {
          // We got an integer as start value
          this.currentValue = new BigDecimal((Integer) start);
        } else if (clazz == Double.class && start.getClass() == Integer.class) {
          // We got an integer as start value
          this.currentValue = new Double((Integer) start);
        } else if (clazz == String.class && start.getClass() == Integer.class) {
          // The integer representation of a string
          // that may be obtains via the {@link StringGenerator.toInt)
          this.currentValue = start;
        } else {
          throw new RuntimeException("The expected class for this generator is not (" + start.getClass() + ") but " + clazz);
        }

      } else {

        if (start.getClass() == String.class) {
          this.currentValue = StringGenerator.toInt((String) start, StringGenerator.MAX_RADIX);
        } else if (start.getClass() == Date.class) {
          this.currentValue = ((Date) start).toLocalDate();
        } else {
          this.currentValue = start;
        }

      }
    }
    return this;
  }

  public SequenceCollectionGenerator<T> step(Integer step) {

    this.step = step;
    return this;

  }

  public SequenceCollectionGenerator max(Integer max) {
    this.maxValue = max;
    return this;
  }

  /**
   * How much data can this generator generate.
   * <p>
   * Example with a start of 0, a step 0f 1 and a maxValue of 2, the maxValue must be 2 (ie 1,2)
   *
   * @return the maxValue number of times the function {@link #getNewValue(ColumnDef)} can be called
   */
  public Long getMaxGeneratedValues() {

    Long maxGeneratedValues;

    if (values != null) {
      maxGeneratedValues = (long) (values.size() / step);
    } else {
      if (clazz == Integer.class || clazz == BigDecimal.class) {
        Integer precisionOrMax = columnDef.getPrecisionOrMax();
        ;
        Integer precision = precisionOrMax != null ? precisionOrMax : MAX_NUMBER_PRECISION;
        maxGeneratedValues = Double.valueOf(Math.pow(10, precision)).longValue();
      } else if (clazz == String.class) {
        Integer precisionOrMax = columnDef.getPrecisionOrMax();
        ;
        Integer precision = precisionOrMax != null ? precisionOrMax : MAX_STRING_PRECISION;
        maxGeneratedValues = Double.valueOf(Math.pow(StringGenerator.MAX_RADIX, precision)).longValue();
      } else if (clazz == Date.class) {
        maxGeneratedValues = DAYS.between((LocalDate) minValue, (LocalDate) maxValue);
      } else {
        throw new RuntimeException("Max Generated Value not implemented for class (" + clazz + ")");
      }
    }
    return maxGeneratedValues;

  }


  /**
   * The sequence will be defined by a list of fix value
   * 1 = first value, 2 = second value ...
   *
   * @param values
   * @return
   */
  public SequenceCollectionGenerator<T> values(List<Object> values) {
    if (clazz == String.class) {
      this.values = values;
      maxValue = this.values.size();
    } else {
      throw new RuntimeException("The values function is not yet implemented for (" + clazz + ")");
    }
    return this;
  }

  @Override
  public String toString() {
    return "SequenceGenerator";
  }
}
