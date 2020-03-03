package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Histogram Distribution Generator known also as enumerated distribution
 * <p>
 * Discrete probability distribution over a finite sample space, based on an enumerated list of <value, probability> pairs.
 * <p>
 * * Probabilities if not given have all the same size for the sample
 * * Probabilities must all be non-negative
 * * Probabilities of zero are allowed
 * * Probabilities sum does not have to equal one, they will be be normalize to make them sum to one.
 * http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
 * <p>
 * See another example at
 * https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/distribution/EnumeratedDistribution.html
 */
public class HistogramCollectionGenerator<T> implements CollectionGeneratorOnce<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HistogramCollectionGenerator.class);

  private final Class<T> clazz;

  private Object o;
  private GenColumnDef columnDef;

  private List<Object> values;

  public HistogramCollectionGenerator(GenColumnDef<T> columnDef, Map<Object, Double> buckets) {

    if (buckets == null) {

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

    if (buckets == null) {
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
    return new HistogramCollectionGenerator<>(genColumnDef, null);
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

    int i = (int) (Math.random() * values.size());
    o = values.get(i);

    try {
      return clazz.cast(o);
    } catch (ClassCastException e) {
      throw new RuntimeException("Unable to cast the value (" + o + ") for the column (" + columnDef + ")", e);
    }
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



  @Override
  public String toString() {
    return "HistogramGenerator{" + columnDef + '}';
  }


}
