package com.tabulify.gen.generator;


import com.tabulify.gen.DataGenType;
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
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.time.temporal.ChronoUnit.DAYS;


/**
 * Distribution Generator that will return a value randomly chosen
 * between a min and a max
 * (ie build a uniform distribution)
 */
public class RandomGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGeneratorScale<T>, CollectionGenerator<T>, java.util.function.Supplier<T> {

  private static final Random random = new Random();

  public static final ChronoUnit TIMESTAMP_UNIT = ChronoUnit.MILLIS;
  public static final ChronoUnit TIME_UNIT = ChronoUnit.MILLIS;

  /**
   * Range = max - min
   */
  private final Number range;
  private StringGenerator stringSequenceGenerator;


  private Object actualValue;


  // Domain
  private final Object min;
  private final Object max;
  private Number step = 1;
  private int numSteps = 0;


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
      range = (((Timestamp) this.max).getTime() - ((Timestamp) this.min).getTime());
      this.actualValue = Timestamp.valueOf(((Timestamp) this.min).toLocalDateTime().plus(((long) range) / 2, TIMESTAMP_UNIT));
      // every second
      step = 1000;
    } else if (Time.class.equals(aClass)) {
      Time minTimeDefault = Time.valueOf("00:00:00");
      Time maxTimeDefault = Time.valueOf("23:59:59");
      this.min = (min != null ? net.bytle.type.time.Time.createFromObject(min).toSqlTime() : clazz.cast(minTimeDefault));
      this.max = (max != null ? net.bytle.type.time.Time.createFromObject(max).toSqlTime() : clazz.cast(maxTimeDefault));
      range = (((Time) this.max).getTime() - ((Time) this.min).getTime());
      this.actualValue = Time.valueOf(((Time) this.min).toLocalTime().plus(((long) range) / 2, TIME_UNIT));
      step = 1000;
    } else if (String.class.equals(aClass) || Character.class.equals(aClass)) {
      stringSequenceGenerator = StringGenerator.builder().build();
      long minCharDefault = stringSequenceGenerator.toInt("a");
      long maxCharDefault = stringSequenceGenerator.toInt("z");
      if (min != null) {
        if (min instanceof String) {
          this.min = stringSequenceGenerator.toInt((String) min);
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
          this.max = stringSequenceGenerator.toInt((String) max);
        } else if (max instanceof Character) {
          this.max = (int) (Character) max;
        } else {
          throw new RuntimeException("The maximum value (" + max + ") defined for the column (" + aClass + ") is a not a string or a character.");
        }
      } else {
        this.max = maxCharDefault;
      }
      range = ((long) this.max - (long) this.min) / (Integer) step;
      this.actualValue = stringSequenceGenerator.toString((long) this.min);
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
  public static <T> RandomGenerator<T> createFromProperties(Class<T> tClass, GenColumnDef<T> genColumnDef) {
    Map<RandomArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(RandomArgument.class);
    Object min = argumentMap.get(RandomArgument.MIN);
    Object max = argumentMap.get(RandomArgument.MAX);

    RandomGenerator<T> generator = (RandomGenerator<T>) (new RandomGenerator<>(tClass, min, max)).setColumnDef(genColumnDef);
    Object stepObject = argumentMap.get(RandomArgument.STEP);
    if (stepObject != null) {
      try {
        generator.setStep(Casts.cast(stepObject, Number.class));
      } catch (CastException e) {
        throw new RuntimeException("The data generator " + SequenceGeneratorArgument.STEP + " value of the column " + genColumnDef + " is not a valid number. Error: " + e.getMessage(), e);
      }
    }
    return generator;
  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    try {

      if (Float.class.equals(this.clazz) || Double.class.equals(this.clazz)) {
        Double intervalDouble = Casts.castSafe(step, Double.class);
        /**
         * TODO: to avoid this condition we should have a builder pattern
         */
        if (numSteps == 0) {
          numSteps = (int) (Casts.castSafe(range, Double.class) / intervalDouble) + 1;
        }
        int randomIndex = random.nextInt(numSteps);
        actualValue = (Double) min + randomIndex * intervalDouble;
        if (clazz.equals(Float.class)) {
          actualValue = Doubles.createFromDouble((Double) actualValue).toFloat();
        }
      } else if (Integer.class.equals(this.clazz)) {// + 0.99 because of the int cast
        // 0 = (int) 0.99
        // 2 = (int) 2.99
        Integer intervalInteger = Casts.castSafe(step, Integer.class);
        /**
         * TODO: to avoid this condition we should have a builder pattern
         */
        if (numSteps == 0) {
          numSteps = (Casts.castSafe(range, Integer.class) / intervalInteger) + 1;
        }
        int randomIndex = random.nextInt(numSteps);
        actualValue = (int) min + ((Integer) step * randomIndex);
      } else {
        GenColumnDef<?> columnDef = this.getColumnDef();
        if (BigDecimal.class.equals(this.clazz)) {
          Double intervalDouble = Casts.castSafe(step, Double.class);
          /**
           * TODO: to avoid this condition we should have a builder pattern
           */
          if (numSteps == 0) {
            numSteps = (int) (Casts.castSafe(range, Double.class) / intervalDouble) + 1;
          }
          int randomIndex = random.nextInt(numSteps);
          double add = intervalDouble * randomIndex;
          BigDecimal bigDecimalNewValue = ((BigDecimal) min)
            .add(BigDecimal.valueOf(add));
          if (columnDef != null) {
            int scale = columnDef.getScale();
            if (scale != 0) {
              bigDecimalNewValue = bigDecimalNewValue.setScale(scale, RoundingMode.HALF_DOWN);
            }
          }
          actualValue = bigDecimalNewValue;
        } else if (Date.class.equals(this.clazz)) {
          /**
           * TODO: to avoid this condition we should have a builder pattern
           */
          if (numSteps == 0) {
            numSteps = Casts.castSafe(range, Integer.class) / (Integer) step + 1;
          }
          int randomIndex = random.nextInt(numSteps);
          int i = randomIndex * (Integer) step;
          LocalDate localValue = ((Date) min).toLocalDate();
          actualValue = Date.valueOf(localValue.plusDays(i));
        } else if (Timestamp.class.equals(this.clazz)) {
          // Count how many multiples exist in the range
          /**
           * TODO: to avoid this condition we should have a builder pattern
           */
          if (numSteps == 0) {
            numSteps = Casts.castSafe(range, Integer.class) / (Integer) step + 1;
          }
          int randomIndex = random.nextInt(numSteps);
          long iTimestamp = (long) randomIndex * (Integer) step;
          LocalDateTime localValueTimestamp = ((Timestamp) min).toLocalDateTime();
          actualValue = Timestamp.valueOf(localValueTimestamp.plus(iTimestamp, TIMESTAMP_UNIT));
        } else if (Time.class.equals(this.clazz)) {
          /**
           * TODO: to avoid this condition we should have a builder pattern
           */
          if (numSteps == 0) {
            numSteps = Casts.castSafe(range, Integer.class) / (Integer) step + 1;
          }
          int randomIndex = random.nextInt(numSteps);
          long randomSecForTime = (long) randomIndex * (Integer) step;
          LocalTime minLocalTime = ((Time) min).toLocalTime();
          actualValue = Time.valueOf(minLocalTime.plus(randomSecForTime, TIME_UNIT));
        } else if (String.class.equals(this.clazz) || Character.class.equals(this.clazz)) {
          /**
           * TODO: to avoid this condition we should have a builder pattern
           */
          if (numSteps == 0) {
            long numSteps = Casts.castSafe(range, Long.class) / (int) step + 1;
            try {
              this.numSteps = Casts.cast(numSteps, Integer.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The number of steps value is too high. It cannot be stored in an integer. Error: " + e.getMessage(), e);
            }
          }
          int randomIndex;
          try {
            randomIndex = random.nextInt(numSteps);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          long iChar = (long) min + (long) randomIndex * (int) step;
          actualValue = stringSequenceGenerator.toString(iChar);
          if (Character.class.equals(this.clazz)) {
            actualValue = ((String) actualValue).charAt(0);
          }
        } else {
          String columnName = columnDef != null ? columnDef.getFullyQualifiedName() : "unknown";
          throw new RuntimeException("The data type with the type code (" + this.clazz.getSimpleName() + ") is not supported (column: " + columnName + ")");
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
   * Example of usage when you have a {@link SequenceGenerator sequence} that have a step that is not one
   * and that you want to foreign data with this generator
   */
  @Override
  public RandomGenerator<T> setStep(Number step) {
    assert step != null : "A step cannot be null";
    boolean isPositive = true;
    if (step instanceof Integer) {
      isPositive = Casts.castSafe(step, Integer.class) > 0;
    } else if (step instanceof Long) {
      isPositive = Casts.castSafe(step, Long.class) > 0;
    } else if (step instanceof Double) {
      isPositive = Casts.castSafe(step, Double.class) > 0;
    }
    assert isPositive : "A step cannot be negative or equal to zero. Actual value is (" + step + ")";
    this.step = step;
    return this;
  }


  public Number getRange() {
    return range;
  }

  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.RANDOM;
  }

  @Override
  public Boolean isNullable() {
    return false;
  }

}
