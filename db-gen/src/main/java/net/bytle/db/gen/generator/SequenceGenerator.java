package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Generate in sequence data (generally used in unique or primary key column)
 *
 * Data Type supported
 *   * integer
 *   * double
 *   * BigDecimal
 *   * date
 *   * string (to support a unique column for instance)
 *
 *
 * Each time, the {@link #getNewValue()} function is called
 * * For integer and date, it will calculate the new value from the start value plus or minValue the step
 *
 *
 * * start (may be a date, a number) - default to 0 / current date
 * * step  integer (number or number of day) - default to +1
 * * maxValue  only active if the step is positive
 * * minValue  only active if the step is negative
 */

public class SequenceGenerator<T> implements CollectionGeneratorOnce<T>, CollectionGeneratorScale<T> {


  private final GenColumnDef<T> columnDef;
  private final Class<T> clazz;


  // The current value that the generated has generated
  private Object currentValue;

  // The start value
  private Object start;
  // The min value is the lower limit value
  private Object minValue;
  // The max value is the upper limit value
  private Object maxValue;
  // The step is the number of step in the sequence (Generally one)
  private int step;


  /**
   * @param columnDef
   */
  public SequenceGenerator(GenColumnDef<T> columnDef) {

    this.columnDef = columnDef;
    this.clazz = columnDef.getClazz();

    if (clazz == Integer.class) {
      currentValue = 0;
      start = 0;
      maxValue = Integer.MAX_VALUE;
      step = 1;
    } else if (clazz == Double.class) {
      currentValue = 0.0;
      start = 0.0;
      maxValue = Double.MAX_VALUE;
      step = 1;
    } else if (clazz == Date.class) {
      currentValue = LocalDate.now();
      start = LocalDate.now();
      step = -1;
      minValue = LocalDate.MIN;
      maxValue = LocalDate.MAX;
    } else if (clazz == BigDecimal.class) {
      currentValue = new BigDecimal(0);
      start = new BigDecimal(0);
      step = 1;
      minValue = Integer.MIN_VALUE;
      maxValue = Integer.MAX_VALUE;
    } else if (clazz == String.class) {
      // Supported to support generation of unique data in a string column
      currentValue = -1;
      start = -1;
      step = 1;
      minValue = 0; // The first code point
    } else {
      throw new UnsupportedOperationException("The class (" + clazz + ") is not supported as a sequence");
    }


  }

  /**
   * The properties of a generator from a data definition file
   *
   * @param columnDef
   * @param <T>
   * @return
   */
  public static <T> SequenceGenerator<T> of(GenColumnDef<T> columnDef) {

    SequenceGenerator<T> sequenceGenerator = new SequenceGenerator<>(columnDef);


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
      Integer precisionOrMax = columnDef.getPrecisionOrMax();
      Integer precision = precisionOrMax != null ? precisionOrMax : MAX_STRING_PRECISION;
      returnValue = SequenceStringGeneratorHelper.toString((Integer) currentValue, SequenceStringGeneratorHelper.MAX_RADIX, precision);
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
      returnValue = SequenceStringGeneratorHelper.toString((Integer) currentValue, SequenceStringGeneratorHelper.MAX_RADIX, columnDef.getPrecision());
    }
    return clazz.cast(returnValue);

  }

  /**
   * @return the column attached to this generator
   */
  @Override
  public GenColumnDef<T> getColumn() {
    return columnDef;
  }


  /**
   * @param start is the start value
   * @return
   */
  public SequenceGenerator<T> start(T start) {

    // When checking the minValue date in a table, the returned value may be null
    // for the sake of simplicity we are not throwing an error
    if (start != null) {

      if (start.getClass() != clazz) {

        if (clazz == BigDecimal.class && start.getClass() == Integer.class) {
          // We got an integer as start value
          this.currentValue = new BigDecimal((Integer) start);
          this.start = new BigDecimal((Integer) start);
        } else if (clazz == Double.class && start.getClass() == Integer.class) {
          // We got an integer as start value
          this.currentValue = new Double((Integer) start);
          this.start = new Double((Integer) start);
        } else if (clazz == String.class && start.getClass() == Integer.class) {
          // The integer representation of a string
          // that may be obtains via the {@link StringGenerator.toInt)
          this.currentValue = start;
          this.start = start;
        } else {
          throw new RuntimeException("The expected class for this generator is not (" + start.getClass() + ") but " + clazz);
        }

      } else {

        if (start.getClass() == String.class) {
          this.currentValue = SequenceStringGeneratorHelper.toInt((String) start, SequenceStringGeneratorHelper.MAX_RADIX);
          this.start = SequenceStringGeneratorHelper.toInt((String) start, SequenceStringGeneratorHelper.MAX_RADIX);
        } else if (start.getClass() == Date.class) {
          this.currentValue = ((Date) start).toLocalDate();
          this.start = ((Date) start).toLocalDate();
        } else {
          this.currentValue = start;
          this.start = start;
        }

      }
    }
    return this;
  }

  /**
   * @param step - the step value
   * @return
   */
  public SequenceGenerator<T> step(Integer step) {

    this.step = step;
    return this;

  }

  /**
   * @param maxSteps - the maximum number of steps
   * @return
   */
  public SequenceGenerator maxSteps(Integer maxSteps) {
    this.maxValue = maxSteps;
    return this;
  }

  /**
   * How much data can this generator generate.
   * <p>
   * Example with a start of 0, a step 0f 1 and a maxValue of 2, the maxValue must be 2 (ie 1,2)
   *
   * @return the maxValue number of times the function {@link #getNewValue} can be called
   */
  public Long getMaxGeneratedValues() {

    Long maxGeneratedValues;
    if (clazz == Integer.class || clazz == BigDecimal.class || clazz == Double.class) {
      Integer precisionOrMax = columnDef.getPrecisionOrMax();
      Integer precision = precisionOrMax != null ? precisionOrMax : MAX_NUMBER_PRECISION;
      maxGeneratedValues = Double.valueOf(Math.pow(10, precision)).longValue();
    } else if (clazz == String.class) {
      Integer precisionOrMax = columnDef.getPrecisionOrMax();
      Integer precision = precisionOrMax != null ? precisionOrMax : MAX_STRING_PRECISION;
      maxGeneratedValues = Double.valueOf(Math.pow(SequenceStringGeneratorHelper.MAX_RADIX, precision)).longValue();
    } else if (clazz == Date.class) {
      maxGeneratedValues = DAYS.between((LocalDate) minValue, (LocalDate) maxValue);
    } else {
      throw new RuntimeException("Max Generated Value not implemented for class (" + clazz + ")");
    }

    return maxGeneratedValues;

  }

  @Override
  public <T> T getDomainMax() {

    if (clazz == Integer.class) {
      Integer max = 0;
      if (maxValue != null) {
        max = (Integer) maxValue;
      }
      Long maxSize = this.getColumn().getDataDef().getSize();
      if (maxSize != null) {
        if (maxSize > max) {
          if (maxSize > Integer.MAX_VALUE) {
            max = Integer.MAX_VALUE;
          } else {
            max = maxSize.intValue();
          }
        }
      }
      return (T) max;
    } else if (clazz == Date.class) {
      if (step < 0) {
        return (T) Date.valueOf(((LocalDate) start));
      } else {
        Long maxSize = this.getColumn().getDataDef().getMaxSize();
        if (maxSize != null) {
          return (T) Date.valueOf(((LocalDate) start).plus(step * maxSize.intValue(), DAYS));
        } else {
          return (T) LocalDate.MAX;
        }
      }
    } else {
      throw new RuntimeException("Domain max on " + clazz + " is not yet implemented");
    }

  }

  @Override
  public <T> T getDomainMin() {
    if (clazz == Integer.class) {
      Integer min = 0;
      if (start != null) {
        min = (Integer) start;
      }
      return (T) min;
    } else if (clazz == Date.class) {
      return (T) minValue;
    } else {
      throw new RuntimeException("Domain min on the class (" + clazz + ") was not yet implemented");
    }
  }


  @Override
  public String toString() {
    return "SequenceGenerator (" + columnDef + ")";
  }

  public int getStep() {
    return step;
  }
}
