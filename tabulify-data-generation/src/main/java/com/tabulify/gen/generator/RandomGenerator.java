package com.tabulify.gen.generator;


import com.tabulify.gen.GenColumnDef;
import net.bytle.exception.CastException;
import net.bytle.type.BigDecimals;
import net.bytle.type.Casts;
import net.bytle.type.Doubles;
import net.bytle.type.Integers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static java.time.temporal.ChronoUnit.DAYS;


/**
 * Distribution Generator that will return a value randomly chosen
 * between a min and a max
 * (ie build an uniform distribution)
 */
public class RandomGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGeneratorScale<T>, CollectionGenerator<T>, java.util.function.Supplier<T> {

  /**
   * Range = max - min
   */
  private final Number range;


  private Object actualValue;


  // Domain
  private final Object min;
  private final Object max;
  private Integer step = 1;


  /**
   * Distribution Generator that will return a value randomly chosen between a min and a max
   *
   * @param aClass the value class
   * @param min    - included
   * @param max    - included
   */
  public RandomGenerator(Class<T> aClass, Object min, Object max) {
    super(aClass);


    if (Double.class.equals(aClass) || Float.class.equals(aClass)) {
      min = Doubles.createFromObject(min).toDouble();
      max = Doubles.createFromObject(max).toDouble();
      this.min = (min != null ? min : 0.0);
      this.max = (max != null ? max : 10.0);
      this.actualValue = ((Double) this.max - (Double) this.min) / 2;
      this.range = ((Double) this.max - (Double) this.min);
    } else if (Integer.class.equals(aClass)) {
      try {
        min = Integers.createFromObject(min).toInteger();
        max = Integers.createFromObject(max).toInteger();
      } catch (CastException e) {
        throw new RuntimeException(e);
      }
      this.min = (min != null ? min : 0);
      this.max = (max != null ? max : 10);
      this.actualValue = ((Integer) this.max - (Integer) this.min) / 2;
      this.range = ((Integer) this.max - (Integer) this.min);
    } else if (BigDecimal.class.equals(aClass)) {
      this.min = (min != null ? BigDecimals.createFromObject(min).toBigDecimal() : BigDecimal.valueOf(0));
      this.max = (max != null ? BigDecimals.createFromObject(max).toBigDecimal() : BigDecimal.valueOf(10));
      this.range = (((BigDecimal) this.max).subtract((BigDecimal) this.min));
      BigDecimal half = ((BigDecimal) this.range).divide(BigDecimal.valueOf(2), RoundingMode.UP);
      this.actualValue = ((BigDecimal) this.min).add(half);
    } else if (Date.class.equals(aClass)) {
      Date minDefault = Date.valueOf(LocalDate.now().minusDays(10));
      Date maxDefault = Date.valueOf(LocalDate.now());
      this.min = (min != null ? net.bytle.type.time.Date.createFromObjectSafeCast(min).toSqlDate() : clazz.cast(minDefault));
      this.max = (max != null ? net.bytle.type.time.Date.createFromObjectSafeCast(max).toSqlDate() : clazz.cast(maxDefault));
      range = ((int) DAYS.between(((Date) this.min).toLocalDate(), ((Date) this.max).toLocalDate()));
      actualValue = Date.valueOf(((Date) this.min).toLocalDate().plusDays((int) range / 2));
    } else if (Timestamp.class.equals(aClass)) {
      Timestamp minTimestampDefault = Timestamp.valueOf(LocalDateTime.now().minusDays(10));
      Timestamp maxTimeStampDefault = Timestamp.valueOf(LocalDateTime.now());
      this.min = (min != null ? net.bytle.type.time.Timestamp.createFromObjectSafeCast(min).toSqlTimestamp() : clazz.cast(minTimestampDefault));
      this.max = (max != null ? net.bytle.type.time.Timestamp.createFromObjectSafeCast(max).toSqlTimestamp() : clazz.cast(maxTimeStampDefault));
      range = (((Timestamp) this.max).getTime() - ((Timestamp) this.min).getTime()) / 1000;
      this.actualValue = Timestamp.valueOf(((Timestamp) this.min).toLocalDateTime().plusSeconds(((long) range) / 2));
    } else if (Time.class.equals(aClass)) {
      Time minTimeDefault = Time.valueOf("00:00:00");
      Time maxTimeDefault = Time.valueOf("23:59:59");
      this.min = (min != null ? net.bytle.type.time.Time.createFromObject(min).toSqlTime() : clazz.cast(minTimeDefault));
      this.max = (max != null ? net.bytle.type.time.Time.createFromObject(max).toSqlTime() : clazz.cast(maxTimeDefault));
      range = (((Time) this.max).getTime() - ((Time) this.min).getTime()) / 1000;
      this.actualValue = Time.valueOf(((Time) this.min).toLocalTime().plusSeconds(((long) range) / 2));
    } else if (String.class.equals(aClass) || Character.class.equals(aClass)) {
      int minCharDefault = SequenceStringGeneratorHelper.toInt("a");
      int maxCharDefault = SequenceStringGeneratorHelper.toInt("z");
      if (min != null) {
        if (min instanceof String) {
          this.min = SequenceStringGeneratorHelper.toInt((String) min);
        } else if (min instanceof Character) {
          this.min = (int) (Character) min;
        } else {
          throw new RuntimeException("The minimum value (" + min + ") defined for the column (" + aClass + ") is a not a string or a character.");
        }
      } else {
        this.min = minCharDefault;
      }
      if (max != null) {
        if (max instanceof String) {
          this.max = SequenceStringGeneratorHelper.toInt((String) max);
        } else if (max instanceof Character) {
          this.max = (int) (Character) max;
        } else {
          throw new RuntimeException("The maximum value (" + max + ") defined for the column (" + aClass + ") is a not a string or a character.");
        }
      } else {
        this.max = maxCharDefault;
      }
      range = ((int) this.max - (int) this.min) / step;
      this.actualValue = SequenceStringGeneratorHelper.toString((int) this.min);
    } else {
      throw new RuntimeException("The class " + aClass + " is not supported by the `random` generator");
    }

  }


  public static <T> RandomGenerator<T> of(Class<T> aClass) {

    return new RandomGenerator<>(aClass, null, null);

  }

  /**
   * Constructor for YAML
   *
   * @param genColumnDef the column where the properties come from
   * @param <T>          the class type
   * @return the generator
   */
  public static <T> RandomGenerator<T> createFromProperties(Class<T> tClass, GenColumnDef genColumnDef) {
    Object min = genColumnDef.getGeneratorProperty(Object.class, "min");
    Object max = genColumnDef.getGeneratorProperty(Object.class, "max");
    return (RandomGenerator<T>) (new RandomGenerator<>(tClass, min, max)).setColumnDef(genColumnDef);
  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    try {

      if (Float.class.equals(this.clazz) || Double.class.equals(this.clazz)) {
        actualValue = Math.random() * (Double) range * step;
        if (min != null) {
          actualValue = (Double) actualValue + (Double) min;
        }
        if (clazz.equals(Float.class)) {
          actualValue = Doubles.createFromDouble((Double) actualValue).toFloat();
        }
      } else if (Integer.class.equals(this.clazz)) {// + 0.99 because of the int cast
        // 0 = (int) 0.99
        // 2 = (int) 2.99
        actualValue = (int) (Math.random() * ((int) range + 0.99) * step);
        if (min != null) {
          actualValue = (int) actualValue + (int) min;
        }
      } else {
        GenColumnDef columnDef = this.getColumnDef();
        if (BigDecimal.class.equals(this.clazz)) {
          BigDecimal bigDecimalNewValue = BigDecimal.valueOf(range.doubleValue());
          bigDecimalNewValue = bigDecimalNewValue
            .multiply(BigDecimal.valueOf(Math.random()))
            .multiply(BigDecimal.valueOf(step));
          if (min != null) {
            bigDecimalNewValue = bigDecimalNewValue.add(((BigDecimal) min));
          }

          if (columnDef != null) {
            Integer scale = columnDef.getScale();
            if (scale != null) {
              bigDecimalNewValue = bigDecimalNewValue.setScale(scale, RoundingMode.HALF_DOWN);
            }
          }
          actualValue = bigDecimalNewValue;
        } else if (Date.class.equals(this.clazz)) {// + 0.99 because of the int cast
          // 0 = (int) 0.99
          // 2 = (int) 2.99
          int i = (int) (Math.random() * ((int) range + 0.99) * step);
          LocalDate localValue = ((Date) min).toLocalDate();
          actualValue = Date.valueOf(localValue.plusDays(i));
        } else if (Timestamp.class.equals(this.clazz)) {
          long iTimestamp = Double.valueOf(Math.random() * (long) range * step).longValue();
          LocalDateTime localValueTimestamp = ((Timestamp) min).toLocalDateTime();
          actualValue = Timestamp.valueOf(localValueTimestamp.plusSeconds(iTimestamp));
        } else if (Time.class.equals(this.clazz)) {
          long randomSecForTime = Double.valueOf(Math.random() * (long) range * step).longValue();
          LocalTime minLocalTime = ((Time) min).toLocalTime();
          actualValue = Time.valueOf(minLocalTime.plusSeconds(randomSecForTime));
        } else if (String.class.equals(this.clazz) || Character.class.equals(this.clazz)) {
          int iChar = (int) min + (int) (Math.random() * ((int) range + 0.99) * step);
          actualValue = SequenceStringGeneratorHelper.toString(iChar);
          if (Character.class.equals(this.clazz)) {
            actualValue = ((String) actualValue).charAt(0);
          }
        } else {
          String columnName = columnDef != null ? columnDef.getFullyQualifiedName() : "unknown";
          throw new RuntimeException("The data type with the type code (" + this.clazz.getSimpleName() + ") is not supported (column: " + columnName);
        }
      }
      return clazz.cast(actualValue);
    } catch (ClassCastException e) {
      throw new RuntimeException("Cast problem for the value (" + actualValue + "), min (" + min + ") for the column (" + this.getColumnDef() + ") against the class (" + clazz.getSimpleName() + ")", e);
    }
  }

  /**
   * @return the actual value
   */
  @Override
  public T getActualValue() {
    return clazz.cast(actualValue);
  }


  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    return new HashSet<>();
  }


  @Override
  public long getCount() {
    return Long.MAX_VALUE;
  }

  @Override
  public void reset() {
    // nothing to do
  }

  @Override
  public T getDomainMax(long size) {
    return getDomainMax();
  }

  @Override
  public T getDomainMax() {
    return Casts.castSafe(max, clazz);
  }

  @Override
  public T getDomainMin(long size) {
    return getDomainMin();
  }

  @Override
  public T getDomainMin() {
    return Casts.castSafe(min, clazz);
  }

  /**
   * @param step - the value of a unit
   * @return You may have a range 0-10 where you pick only the even number
   * A step would be in this case 2
   * @deprecated - Step is not fully functional (not really deprecated but not functional)
   * Example of usage when you have a {@link SequenceGenerator sequence} that have a step that is not one
   * and that you want to foreign data with this generator
   */
  @Deprecated
  @Override
  public RandomGenerator<T> setStep(Number step) {
    assert step != null : "A step cannot be null";
    this.step = Casts.castSafe(step, Integer.class);
    assert this.step > 0 : "A step cannot be negative or equal to zero. Actual value is (" + step + ")";
    return this;
  }


  public Number getRange() {
    return range;
  }
}
