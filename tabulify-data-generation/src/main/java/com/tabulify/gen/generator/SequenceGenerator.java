package com.tabulify.gen.generator;


import com.tabulify.gen.GenColumnDef;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.model.UniqueKeyDef;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Arrayss;
import net.bytle.type.Casts;
import net.bytle.type.Doubles;
import net.bytle.type.Typess;
import net.bytle.type.time.Date;
import net.bytle.type.time.Timestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

/**
 * Generate in sequence data (generally used in unique or primary key column)
 * <p>
 * Data Type supported
 * * integer
 * * double
 * * BigDecimal
 * * date
 * * string (to support a unique column for instance)
 * <p>
 * This sequence generator supports only a integer as unit.
 * For date, this sequence support only the day unit (ie
 * * 1 step = 1 day
 * * 1 step = 1 code character
 * * ...
 * If we want to implement a sequence of one month, we need to support the concept of unit (ie create
 * another implementation with the {@link ChronoUnit}
 * <p>
 * <p>
 * Each time, the {@link #getNewValue()} function is called
 * * For integer and date, it will calculate the new value from the start value plus or minValue the step
 * <p>
 * <p>
 * * start (may be a date, a number) - default to 0 / current date
 * * step  integer (number or number of day) - default to +1
 * * maxValue  only active if the step is positive
 * * minValue  only active if the step is negative
 */

public class SequenceGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGeneratorScale<T>, CollectionGenerator<T>, Supplier<T> {


  public static final int DEFAULT_TIMESTAMP_STEP = -10000;

  /**
   * The name of this generator used in the test to create yaml mapping
   */
  @SuppressWarnings("unused")
  public static final String NAME = "sequence";

  /**
   * A sequence of objects that contains the
   * values given back by the function {@link #getNewValue()}
   * If this sequence is null, a generated sequence by data type is taken
   */
  private List<T> values;


  /**
   * The start value of the sequence
   */
  private T start;

  /**
   * The offset from the start
   * For instance, if you select the date 2021-01-01, you would
   * set the offset at -1 to start with the date 2020-12-31
   */
  private Number offset = 0;

  /**
   * The size of a step
   * for each tick
   * <p>
   * Generally:
   * * 1 for numeric and char
   * * -1 for date
   */

  private Number stepSize;

  /**
   * A counter (if i=1, one new value was asked, if i=2, two new values were asked)
   * The counter advance of 1 for every tick
   */
  private int tickCounter = 0;

  /**
   * The actual value
   */
  private T actualValue;

  /**
   * The maximum number of tick.
   * A tick will produce a new value
   * By default, there is one tick when calling {@link #getNewValue()}
   * but a tick may be generated externally
   */
  private Long maxTickCount = Long.MAX_VALUE;

  /**
   * The ticker is an external generator that
   * will call the {@link #incrementTickCounter()}
   * <p>
   * By default, there is no ticker
   * There is therefore a tick on each call of {@link #getNewValue()}
   */
  private SequenceGenerator<?> tickerFor;

  /**
   * Generator that will tick this
   */
  private SequenceGenerator<?> tickedBy;
  /**
   * Reset the sequence
   */
  private boolean reset = false;


  /**
   * @param clazz type of data to generate
   */
  public SequenceGenerator(Class<T> clazz) {
    super(clazz);
  }

  /**
   * The properties of a generator from a data definition file
   */

  public static <T> SequenceGenerator<T> create(Class<T> clazz) {

    SequenceGenerator<T> sequenceGenerator = new SequenceGenerator<>(clazz);

    Object start;
    Number offset = 0;
    Number step;
    if (clazz == Integer.class) {
      start = 1;
      step = 1;
    } else if (clazz == Double.class) {
      start = 1.0;
      step = 1.0;
    } else if (clazz == Float.class) {
      start = (float) 1.0;
      step = (float) 1.0;
    } else if (clazz == java.sql.Date.class) {
      start = Date.createFromNow().toSqlDate();
      step = -1;
      offset = -1;
    } else if (clazz == java.sql.Timestamp.class) {
      start = Timestamp.createFromNowLocalSystem().toSqlTimestamp();
      step = DEFAULT_TIMESTAMP_STEP; // ms
    } else if (clazz == BigDecimal.class) {
      start = new BigDecimal(0);
      step = 1;
    } else if (clazz == String.class) {
      // Supported to support generation of unique data in a string column
      start = "a";
      step = 1;
    } else {
      throw new UnsupportedOperationException("The class (" + clazz.getSimpleName() + ") is not supported as a sequence");
    }

    sequenceGenerator.setStart(clazz.cast(start));
    sequenceGenerator.setOffset(offset);
    sequenceGenerator.setStep(step);


    return sequenceGenerator;

  }

  public SequenceGenerator<T> setOffset(Number offset) {
    this.offset = offset;
    return this;
  }

  /**
   * This is the function that's called recursively at the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   */
  public static <T> SequenceGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef columnDef) {

    SequenceGenerator<T> sequenceGenerator = (SequenceGenerator<T>) create(clazz)
      .setColumnDef(columnDef);

    final Number stepObj = columnDef.getGeneratorProperty(Number.class, "step");
    if (stepObj != null) {
      sequenceGenerator.setStep(stepObj);
      if (columnDef.getClazz().equals(java.sql.Date.class)) {
        if (Doubles.createFromObject(stepObj).toDouble() > 0.0) {
          /**
           * No negative offset when going forward
           */
          sequenceGenerator.setOffset(0);
        }
      }
    }
    final Integer offset = columnDef.getGeneratorProperty(Integer.class, "offset");
    if (offset != null) {
      sequenceGenerator.setOffset(offset);
    }

    final T startObj = columnDef.getGeneratorProperty(clazz, "start");
    if (startObj != null) {
      sequenceGenerator.setStart(startObj);
    }

    final Boolean reset = columnDef.getGeneratorProperty(Boolean.class, "reset");
    if (reset != null) {
      sequenceGenerator.setReset(reset);
    }

    Object valuesProperty = columnDef.getGeneratorProperty(Object.class, "values");
    if (valuesProperty != null) {
      List<T> valuesList;
      if (valuesProperty instanceof List) {
        valuesList = Casts.castToListSafe(valuesProperty, clazz);
      } else {
        throw new RuntimeException("The values (" + valuesProperty + ") of the sequence generator for the column (" + columnDef + ") are not a list but are a " + valuesProperty.getClass() + ". A list should be provided");
      }
      sequenceGenerator.setValues(valuesList);
    }

    Long maxTickProperty = columnDef.getGeneratorProperty(Long.class, "maxTick");
    if (maxTickProperty != null) {
      try {
        sequenceGenerator.setMaxTick(maxTickProperty);
      } catch (ClassCastException e) {
        throw new RuntimeException("The max tick property for the data generator of the column (" + columnDef.getFullyQualifiedName() + ") is not an number (Value: " + maxTickProperty + ")", e);
      }
    }

    String tickerFor = columnDef.getGeneratorProperty(String.class, "tickerFor");
    if (tickerFor != null) {
      sequenceGenerator.setTickerFor(tickerFor);
    }

    return sequenceGenerator;

  }


  @SuppressWarnings("unchecked")
  public static <T> SequenceGenerator<T> createFromValues(Class<T> clazz, T... values) {

    SequenceGenerator<T> sequenceGenerator = create(clazz);
    sequenceGenerator.setValues(Arrays.asList(values));
    return sequenceGenerator;
  }

  /**
   * An utility function that create sequence generators
   * with tickers relation on several columns
   * This is mostly used to create a generator on:
   * * {@link UniqueKeyDef#getColumns()} and
   * * {@link PrimaryKeyDef#getColumns()}
   *
   * @param columnDefs - the series of columns
   */
  public static void createOdometer(List<GenColumnDef> columnDefs) {
    int index = 0;
    for (GenColumnDef genColumnDef : columnDefs) {
      if (index == 0) {
        genColumnDef.addSequenceGenerator(genColumnDef.getClazz());
      } else {
        genColumnDef.addSequenceGenerator(genColumnDef.getClazz())
          .setTickerFor(columnDefs.get(index - 1).getColumnName());
      }
      index++;
    }
  }


  /**
   * We have two parameters to be sure that
   * we get the good class of object
   *
   * @param value  the value
   * @param values the values
   */
  @SafeVarargs
  private SequenceGenerator<T> setValues(T value, T... values) {

    if (value instanceof Collection || value.getClass().isArray()) {
      if (values.length == 0) {
        return setValues(Casts.castToListSafe(value, this.clazz));
      } else {
        throw new RuntimeException("The first value is a collection and the section vargs values are not empty. This is not permitted");
      }
    } else {
      return setValues(Arrays.asList(Arrayss.concat(value, values)));
    }
  }

  private SequenceGenerator<T> setValues(List<T> values) {
    this.values = values;
    this.maxTickCount = (long) (values.size());
    this.start = null;
    return this;
  }

  /**
   * @return a new generated data object every time it's called
   * The next value is always the start value + the step
   * This is because when you insert data in a primary key column, you will give the maxValue and this generator
   * will give you the next value.
   */
  @Override
  public T getNewValue() {

    Object localValue;
    if (this.values != null) {
      if (tickCounter < this.values.size()) {
        localValue = this.values.get(tickCounter);
      } else {
        throw new RuntimeException("The tick counter is greater than the number of values available. Did you forgot to reset the sequence ?");
      }
    } else {
      if (getColumnDef().getClazz() == Integer.class) {
        localValue = (Integer) (start) + Casts.castSafe(offset, Integer.class) + tickCounter * Casts.castSafe(stepSize, Integer.class);
      } else if (getColumnDef().getClazz() == Double.class) {
        localValue = (Double) (start) + Casts.castSafe(offset, Double.class) + tickCounter * Casts.castSafe(stepSize, Double.class);
      } else if (getColumnDef().getClazz() == Float.class) {
        localValue = (Float) (start) + Casts.castSafe(offset, Float.class) + tickCounter * Casts.castSafe(stepSize, Float.class);
      } else if (getColumnDef().getClazz() == java.sql.Date.class) {
        localValue = Date.createFromObjectSafeCast(start).plusDays(Casts.castSafe(offset, Integer.class) + (long) tickCounter * Casts.castSafe(stepSize, Integer.class)).toSqlDate();
      } else if (getColumnDef().getClazz() == java.sql.Timestamp.class) {
        localValue = Timestamp.createFromObjectSafeCast(start).afterMillis(Casts.castSafe(offset, Integer.class) + (long) tickCounter * Casts.castSafe(stepSize, Integer.class)).toSqlTimestamp();
      } else if (getColumnDef().getClazz() == BigDecimal.class) {
        localValue = ((BigDecimal) (start)).add(BigDecimal.valueOf(Casts.castSafe(offset, Double.class))).add(BigDecimal.valueOf(Casts.castSafe(stepSize, Double.class)).multiply(new BigDecimal(tickCounter)));
        Integer scale = this.getColumnDef().getScale();
        if (scale != null) {
          localValue = ((BigDecimal) localValue).setScale(scale, RoundingMode.HALF_DOWN);
        }
      } else if (getColumnDef().getClazz() == String.class) {
        int startI = SequenceStringGeneratorHelper.toInt((String) start);
        int codePoint = (startI) + Casts.castSafe(offset, Integer.class) + this.tickCounter * Casts.castSafe(stepSize, Integer.class);
        localValue = SequenceStringGeneratorHelper.toString(codePoint);
      } else {
        throw new RuntimeException("The data type (" + getColumnDef().getClazz() + ") is not yet implemented for a sequence generator");
      }
    }
    try {
      this.actualValue = clazz.cast(localValue);
    } catch (Exception e) {
      throw new InternalException("Cast class type inconsistency for the sequence generator (" + this + "). Error:" + e.getMessage(), e.getCause());
    }

    /**
     * If there is no ticker, increment the counter each time
     */
    if (tickedBy == null) {
      incrementTickCounter();
    }

    return this.actualValue;


  }

  /**
   * Just tick this sequence generator
   * Process the increment of the counter
   */
  private SequenceGenerator<T> tick() {
    incrementTickCounter();
    return this;
  }

  /**
   * Process a tick
   */
  private void incrementTickCounter() {

    tickCounter++;

    if (tickCounter >= maxTickCount && reset) {
      internalReset();
      if (tickerFor != null) {
        tickerFor.tick();
      }
    }

  }

  /**
   * Reset is an external function
   * that reset the state when  a stream is closed.
   * To avoid conflict with the reset function of the sequence
   * we have created this function
   */
  private void internalReset() {

    if (reset) {
      reset();
    } else {
      throw new IllegalStateException("The `reset` property is false, therefore we can't reset this sequence");
    }

  }

  /**
   * @return a generated value
   */
  @Override
  public T getActualValue() {

    return this.actualValue;

  }

  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    if (this.tickerFor != null) {
      return Collections.singleton(this.tickerFor);
    } else {
      return new HashSet<>();
    }
  }


  /**
   * @param start is the start value
   * @return the generator for chaining
   */
  public SequenceGenerator<T> setStart(T start) {
    this.start = start;
    this.actualValue = start;
    return this;
  }

  /**
   * This is generally:
   * * +1 for number and list
   * * -1 for date
   *
   * @param step - the step value
   * @return the object for chaining
   */
  public SequenceGenerator<T> setStep(Number step) {

    this.stepSize = step;
    return this;

  }


  /**
   * How much data can this generator should generate
   *
   * @return the maxValue number of times the function {@link #getNewValue} can be called
   * <p>
   * By default, {@link Long#MAX_VALUE}
   */
  public long getSizeWithoutBeingTicked() {

    /**
     * Max Size data type based
     */
    Long maxByClassSize = Typess.getMaxByClass(this.clazz);

    /**
     * Max Size caused by the precision
     * ie a precision of 3
     */
    Long maxPrecisionSize = null;
    GenColumnDef columnDef = getColumnDef();
    if (clazz == Integer.class || clazz == BigDecimal.class || clazz == Double.class) {
      Integer precisionOrMax = columnDef != null ? columnDef.getPrecisionOrMax() : null;
      maxPrecisionSize = precisionOrMax != null ?
        Double.valueOf(Math.pow(10, precisionOrMax)).longValue() - 1
        : MAX_NUMBER_PRECISION;
    } else if (clazz == String.class) {
      Integer precisionOrMax = columnDef != null ? columnDef.getPrecisionOrMax() : null;
      maxPrecisionSize = precisionOrMax != null ?
        Double.valueOf(Math.pow(SequenceStringGeneratorHelper.MAX_RADIX, precisionOrMax)).longValue()
        : MAX_STRING_PRECISION;
    }

    Long maxSize = maxByClassSize;
    if (maxPrecisionSize != null && maxPrecisionSize < maxSize) {
      maxSize = maxPrecisionSize;
    }


    if (this.values != null) {
      if (this.values.size() < maxSize) {
        maxSize = (long) this.values.size();
      }
    }

    /**
     * Tick calculation
     * Without a ticker, the max size is equal
     * to the number of tick
     */
    if (this.maxTickCount != null) {
      if (maxSize > this.maxTickCount) {
        maxSize = this.maxTickCount;
      }
    }
    if (this.tickerFor != null) {
      /**
       * With a ticker, we need to multiply by the
       * size of the ticker
       */
      long tickerSize = this.tickerFor.getSizeWithoutBeingTicked();
      maxSize = maxSize * tickerSize;
    }

    /**
     * If reset on a column that is not a ticker
     */
    if (this.tickerFor == null && this.reset) {

      maxSize = maxByClassSize;

    }


    /**
     * Capping on data path max size
     */
    if (this.getRelationDef().getDataPath().getMaxRecordCount() != null && maxSize > this.getRelationDef().getDataPath().getMaxRecordCount()) {
      maxSize = this.getRelationDef().getDataPath().getMaxRecordCount();
    }


    return maxSize;

  }

  @Override
  public long getCount() {
    /**
     * If this generator is not externally ticked
     */
    long maxSize = getSizeWithoutBeingTicked();
    if (this.tickedBy != null) {
      maxSize = this.tickedBy.getCount();
    }
    return maxSize;

  }

  @Override
  public void reset() {
    // counter at zero
    tickCounter = 0;
  }

  @Override
  public T getDomainMax(long maxSteps) {
    if (start instanceof Number) {
      long ticks = maxSteps;
      if (maxTickCount != null && maxTickCount < maxSteps) {
        ticks = maxTickCount;
      }
      if (clazz == Integer.class) {
        return clazz.cast(Casts.castSafe(ticks - 1, Integer.class) * Casts.castSafe(stepSize, Integer.class) + Casts.castSafe(start, Integer.class));
      } else if (clazz == Float.class) {
        return clazz.cast(Casts.castSafe(ticks - 1, Float.class) * Casts.castSafe(stepSize, Float.class) + Casts.castSafe(start, Float.class));
      } else if (clazz == Double.class) {
        return clazz.cast(Casts.castSafe(ticks - 1, Double.class) * Casts.castSafe(stepSize, Double.class) + Casts.castSafe(start, Double.class));
      } else {
        throw new RuntimeException("The domain max for the data of clazz type (" + clazz + ") is not yet implemented");
      }
    } else if (clazz == java.sql.Date.class) {
      if (Casts.castSafe(stepSize, Integer.class) < 0) {
        return clazz.cast(Date.createFromObjectSafeCast(start).plusDays(Casts.castSafe(offset, Integer.class)).toSqlDate());
      } else {
        if (maxSteps != Long.MAX_VALUE) {
          java.sql.Date max = Date.createFromObjectSafeCast(start).plusDays((long) Casts.castSafe(stepSize, Integer.class) * (int) maxSteps + Casts.castSafe(offset, Integer.class) - 1).toSqlDate();
          return Casts.castSafe(max, clazz);
        } else {
          return Casts.castSafe(java.sql.Date.valueOf(LocalDate.MAX), clazz);
        }
      }
    } else if (clazz == java.sql.Timestamp.class) {
      if (Casts.castSafe(stepSize, Integer.class) < 0) {
        return clazz.cast(start);
      } else {
        if (maxSteps != Long.MAX_VALUE) {
          java.sql.Timestamp max = Timestamp.createFromObjectSafeCast(start).afterMillis((long) Casts.castSafe(stepSize, Integer.class) * (int) maxSteps + Casts.castSafe(offset, Integer.class)).toSqlTimestamp();
          return clazz.cast(max);
        } else {
          return clazz.cast(new java.sql.Timestamp(Long.MAX_VALUE));
        }
      }
    } else {
      throw new RuntimeException("Domain max on " + clazz + " is not yet implemented");
    }

  }

  @Override
  public T getDomainMax() {
    long maxSteps = getCount();
    return getDomainMax(maxSteps);
  }

  @Override
  public T getDomainMin() {
    long size = getCount();
    return getDomainMin(size);
  }

  /**
   *
   */
  @Override
  public T getDomainMin(long size) {

    if (start instanceof Number) {
      // (stepSize) * (size - 1)
      return clazz.cast(start);
    } else {
      if (clazz.equals(java.sql.Date.class)) {
        if (Casts.castSafe(stepSize, Integer.class) > 0) {
          return clazz.cast(start);
        } else {
          if (size == Long.MAX_VALUE) {
            /**
             * The min is at 0 for 1970
             */
            return clazz.cast(new java.sql.Date(0));
          } else {
            return clazz.cast(Date.createFromObjectSafeCast(start).plusDays((Casts.castSafe(stepSize, Integer.class)) * (size - 1) + Casts.castSafe(offset, Integer.class)).toSqlDate());
          }
        }
      } else if (clazz.equals(java.sql.Timestamp.class)) {
        if (Casts.castSafe(stepSize, Integer.class) > 0) {
          return clazz.cast(start);
        } else {
          if (size == Long.MAX_VALUE) {
            /**
             * The min is at 0 for 1970
             */
            return clazz.cast(new java.sql.Timestamp(0));
          } else {
            return clazz.cast(Timestamp.createFromObjectSafeCast(start).beforeMillis((Math.abs(Casts.castSafe(stepSize, Integer.class))) * (size - 1) + Casts.castSafe(offset, Integer.class)).toSqlTimestamp());
          }
        }
      } else {
        throw new RuntimeException("Domain min on the class (" + clazz + ") was not yet implemented");
      }
    }
  }


  public Number getStepSize() {
    return stepSize;
  }

  /**
   * @param tickedColumnName - the column name, ie sequence generator that should be ticked by this generator at the end of the sequence
   * @return the object for chaining
   */
  public SequenceGenerator<T> setTickerFor(String tickedColumnName) {

    GenColumnDef columnDef;
    try {
      columnDef = this.getRelationDef().getColumnDef(tickedColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The ticked column (" + tickedColumnName + ") could not be found in the data path (" + this.getRelationDef().getDataPath() + ")");
    }
    CollectionGenerator<?> parentGenerator = columnDef.getOrCreateGenerator(columnDef.getClazz());
    if (parentGenerator instanceof SequenceGenerator) {
      this.tickerFor = (SequenceGenerator<?>) parentGenerator;
      this.tickerFor.addTickedBy(this);
      this.reset = true;
    } else {
      throw new IllegalStateException("The parent column (" + tickedColumnName + ") has not a sequence generator but a (" + parentGenerator.getGeneratorType() + ") and can't therefore be ticked. Set a sequence generator to the column (" + tickedColumnName + ") or change the column name.");
    }

    return this;
  }

  /**
   * The sequence generator that will tick this generator
   */
  private SequenceGenerator<T> addTickedBy(SequenceGenerator<?> sequenceGenerator) {
    this.tickedBy = sequenceGenerator;
    return this;
  }

  /**
   * The number of times a tick (next value is generated) occurs.
   * <p>
   * This is a maximum value on the internal {@link #tickCounter}
   * <p>
   * By default {@link Long#MAX_VALUE}
   */
  public SequenceGenerator<T> setMaxTick(Long maxTickCount) {
    this.maxTickCount = maxTickCount;
    return this;
  }

  public T getStart() {
    return start;
  }

  /**
   * @return the maximum number of tick
   */
  public long getMaxTick() {
    return this.maxTickCount;
  }

  /**
   * The generator that ticks this sequence
   */
  public SequenceGenerator<?> getTickedBy() {
    return this.tickedBy;
  }

  /**
   * The generator that must be ticked by this sequence
   */
  public SequenceGenerator<?> getTickerFor() {
    return this.tickerFor;
  }

  /**
   * The offset from the start used to start a date before than the first day of a month.
   */
  public Number getOffset() {
    return offset;
  }

  public SequenceGenerator<T> setReset(boolean reset) {
    this.reset = reset;
    return this;
  }


  public boolean getReset() {
    return reset;
  }
}
