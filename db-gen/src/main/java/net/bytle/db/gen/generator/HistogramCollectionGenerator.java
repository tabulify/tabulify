package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;


/**
 * Histogram Distribution Generator known also as enumerated distribution
 *
 * Discrete probability distribution over a finite sample space, based on an enumerated list of <value, probability> pairs.
 *
 *   * Probabilities if not given have all the same size for the sample
 *   * Probabilities must all be non-negative
 *   * Probabilities of zero are allowed
 *   * Probabilities sum does not have to equal one, they will be be normalize to make them sum to one.
 * http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
 *
 * See another example at
 * https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/distribution/EnumeratedDistribution.html
 */
public class HistogramCollectionGenerator<T> implements CollectionGeneratorOnce<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HistogramCollectionGenerator.class);

  private final Class<T> clazz;

  private Object o;
  private GenColumnDef columnDef;

  // Domain
  private Object min;
  private Object max;
  private int step;

  private Object range;
  private List<Object> values;

  public HistogramCollectionGenerator(GenColumnDef<T> columnDef, Map<Object,Double> buckets) {

    if (buckets==null) {

      final Object bucketsObject = columnDef.getProperty("buckets");
      try {
        buckets = (Map<Object, Double>) bucketsObject;
      } catch (ClassCastException e) {
        throw new RuntimeException("The buckets definition of the column (" + columnDef.getFullyQualifiedName() + ") are not in the map format <Object,Integer>. The values are: " + bucketsObject);
      }

      // DataType Check
      if (buckets != null) {
        Object o = buckets.entrySet().iterator().next().getKey();
        if (o.getClass() != columnDef.getDataType().getClazz()) {
          throw new RuntimeException("The data type of the key with the the value (" + o + ") in the buckets definition of the column " + columnDef.getFullyQualifiedName() + " is not a " + columnDef.getDataType().getClazz().getSimpleName() + " but a " + o.getClass().getSimpleName() + ".");
        }

      }
    }

    if (buckets==null){
      throw new RuntimeException("You can't create a custom probability distribution without bucket definition");
    }

    // Create the values list and add the element according to their ratio
    // A ratio of 3 = 3 elements in the list
    values = new ArrayList<>();
    for (Map.Entry<Object, Double> entry : buckets.entrySet()) {
      for (int i = 0; i < entry.getValue(); i++) {
        values.add(entry.getKey());
      }
    }

    this.columnDef = columnDef;
    clazz = columnDef.getClazz();
    int typeCode = columnDef.getDataType().getTypeCode();
    switch (typeCode) {
      case (Types.DOUBLE):
      case Types.FLOAT:
        // Other name for double
        range = 10.0;
        min = 0.0;
        break;
      case Types.INTEGER:
        range = 10;
        min = 0;
        break;
      case Types.VARCHAR:
      case Types.CHAR:
        o = getString();
        break;
      case Types.NUMERIC:
        range = BigDecimal.valueOf(10);
        min = BigDecimal.valueOf(0);
        break;
      case Types.DATE:
        o = Date.valueOf(LocalDate.now());
        range = 10;
        min = Date.valueOf(LocalDate.now().minusDays((int) range));
        max = Date.valueOf(LocalDate.now());
        break;
      case Types.TIMESTAMP:
        o = Timestamp.valueOf(LocalDateTime.now());
        range = 10;
        min = Timestamp.valueOf(LocalDateTime.now().minusDays((int) range));
        max = Timestamp.valueOf(LocalDateTime.now());
        break;
      default:
        throw new RuntimeException("The data type with the type code (" + typeCode + "," + clazz.getSimpleName() + ") is not supported for the column " + columnDef.getFullyQualifiedName());

    }

  }

  /**
   * The buckets defines the distribution of discrete variable
   * where:
   * * The discrete variable are in the first Object variable
   * * The ratios are in the second Integer variable
   * <p>
   * Example with the following, you will have 2 times much Red than Blue and Green
   * Blue: 1
   * Red: 2
   * Green: 1
   *
   * @param buckets
   * @return
   */
  public static <T> HistogramCollectionGenerator<T> of(GenColumnDef<T> columnDef, Map<Object, Double> buckets) {

    return new HistogramCollectionGenerator<>(columnDef, buckets);

  }

  public static <T> HistogramCollectionGenerator<T> of(GenColumnDef genColumnDef) {
    return new HistogramCollectionGenerator<>(genColumnDef,null);
  }


  private String getString() {
    Integer precision = this.columnDef.getPrecisionOrMax();
    if (precision == null) {
      precision = CollectionGeneratorOnce.MAX_STRING_PRECISION;
      LOGGER.warn(
        Strings.multiline("The precision for the column (" + this.columnDef + ") is unknown",
          "The max precision for its data type (" + columnDef.getDataType().getTypeName() + ") is unknown",
          "The precision was then set to " + precision
        ));
    }

    String s = "hello";
    if (s.length() > precision) {
      s = s.substring(0, precision);
    }
    return s;
  }

  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    if (values == null) {
      switch (columnDef.getDataType().getTypeCode()) {
        case Types.DOUBLE:
          o = Math.random() * (Double) range;
          if (min != null) {
            o = (Double) o + (Double) min;
          }
          break;
        case Types.INTEGER:
          o = (int) (Math.random() * (int) range);
          if (min != null) {
            o = (int) o + (int) min;
          }
          break;
        case Types.NUMERIC:
          o = BigDecimal.valueOf(Math.random() * ((BigDecimal) range).doubleValue());
          if (min != null) {
            o = ((BigDecimal) o).add(((BigDecimal) min));
          }
          break;
        case Types.DATE:
          int i = (int) (Math.random() * (int) range);
          LocalDate localValue = ((Date) min).toLocalDate();
          o = Date.valueOf(localValue.plusDays(i));
          break;
        case Types.TIMESTAMP:
          int iTimestamp = (int) (Math.random() * (int) range);
          LocalDateTime localValueTimestamp = ((Timestamp) min).toLocalDateTime();
          o = Timestamp.valueOf(localValueTimestamp.plusDays(iTimestamp));
          break;

      }

    } else {

      int i = (int) (Math.random() * values.size());
      o = values.get(i);

    }


    return clazz.cast(o);
  }

  /**
   * @return a generated value (used in case of derived data
   */
  @Override
  public T getActualValue() {
    return clazz.cast(o);
  }

  /**
   * @return the column attached to this generator
   * It permits to create parent relationship between generators
   * when asking a value for a column, we may need to ask the value for another column before
   */
  @Override
  public GenColumnDef<T> getColumn() {

    return columnDef;

  }



  @Override
  public long getMaxGeneratedValues() {
    return Long.MAX_VALUE;
  }


  public HistogramCollectionGenerator<T> setMin(T min) {
    if (min != null) {
      this.min = min;
      updateRange();
    }
    return this;
  }

  public HistogramCollectionGenerator<T> setMax(T max) {
    if (max != null) {
      this.max = max;
      updateRange();
    }

    return this;
  }

  private void updateRange() {
    // Range
    switch (columnDef.getDataType().getTypeCode()) {
      case Types.DOUBLE:
        if (max != null) {
          range = (double) max - (double) min;
        }
        break;
      case Types.INTEGER:
        if (max != null) {
          range = (int) max - (int) min;
        }
        break;
      case Types.NUMERIC:
        if (max != null) {
          range = ((BigDecimal) max).min((BigDecimal) min);
        }
        break;
      case Types.DATE:
        if (max != null) {
          range = (int) DAYS.between(((Date) min).toLocalDate(), ((Date) max).toLocalDate());
        }
        break;

    }
  }

  @Override
  public String toString() {
    return "DistributionGenerator{" + columnDef + '}';
  }

  public HistogramCollectionGenerator setStep(int step) {
    this.step = step;
    return this;
  }
}
