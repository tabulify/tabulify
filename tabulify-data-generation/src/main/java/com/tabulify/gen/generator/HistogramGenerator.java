package com.tabulify.gen.generator;


import com.tabulify.gen.GenColumnDef;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Histogram Distribution Generator known also as enumerated distribution
 * <p>
 * Discrete probability distribution over a finite sample space, based on an enumerated list of <value, probability> pairs.
 * <p>
 * * Probabilities if not given have all the same size for the sample
 * * Probabilities must all be non-negative
 * * Probabilities of zero are allowed
 * * Probabilities sum does not have to equal one, they will be normalize to make them sum to one.
 * <a href="http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution">...</a>
 * <p>
 * See another example at
 * <a href="https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/distribution/EnumeratedDistribution.html">...</a>
 */
public class HistogramGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {


  public static final String TYPE = "histogram";


  private T actualValue;

  private final List<Object> values;

  /**
   * @param buckets - the buckets where the data needs to be generated
   * @param clazz   - the return clazz of the value - this is used from the {@link DataSetGenerator} where
   *                an histogram of the row (long) is used to choose the value
   */
  public HistogramGenerator(Class<T> clazz, Map<T, Double> buckets) {

    super(clazz);


    if (buckets == null) {
      throw new RuntimeException("You can't create a custom probability distribution without bucket definition");
    }

    // Create the values list and add the element according to their ratio
    // A ratio of 3 = 3 elements in the list
    values = new ArrayList<>();
    for (Map.Entry<T, Double> entry : buckets.entrySet()) {
      Object key = entry.getKey();
      if (key == null) {
        throw new RuntimeException("An histogram bucket cannot contain a null key");
      }
      Double value = entry.getValue();
      if (value == null) {
        throw new RuntimeException("An histogram bucket cannot contain a null value for the key (" + key + ")");
      }
      for (int i = 0; i < value; i++) {
        values.add(key);
      }
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
   * @param buckets - the buckets where to generate the data
   * @return the histogram
   */
  public static <T> HistogramGenerator<T> create(Class<T> clazz, Map<T, Double> buckets) {
    return new HistogramGenerator<>(clazz, buckets);
  }

  /**
   * Instantiate an expression generator from the columns properties
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   */
  public static <T> HistogramGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {

    Map<HistogramArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(HistogramArgument.class);
    Object objectBucket = argumentMap.get(HistogramArgument.BUCKETS);
    if (objectBucket == null) {
      throw new RuntimeException("The buckets column property is mandatory and was not found for the column (" + genColumnDef + ")");
    }
    Map<T, Double> buckets;
    if (objectBucket instanceof Map) {
      try {
        // Strict: ie
        // Key: 2.0 on an integer clazz will return an error (ie strictKey = true)
        // Values: 2 should return an error as it's not a Double, but we do not as it's the castToNewMap and not castToSameMap
        // time is not a yaml format
        boolean strictKey = !clazz.equals(java.sql.Time.class);
        buckets = Casts.castToNewMap(objectBucket, clazz, Double.class, strictKey);
      } catch (CastException e) {
        throw new RuntimeException("The data generator buckets column property for the column (" + genColumnDef + ") is not a map of " + clazz.getSimpleName() + ", Double. Error: " + e.getMessage(), e);
      }
    } else if (objectBucket instanceof Collection) {
      buckets = Casts.castToNewListSafe(objectBucket, clazz)
        .stream()
        .collect(Collectors.toMap(
          s -> s,
          s -> 1.0
        ));
    } else {
      throw new RuntimeException("The `buckets` value (" + objectBucket + ") of the column (" + genColumnDef + ") are not a list or a map");
    }
    return new HistogramGenerator<>(clazz, buckets);

  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    int i = (int) (Math.random() * values.size());
    actualValue = Casts.castSafe(values.get(i), clazz);

    return actualValue;

  }

  /**
   * @return the actual value
   */
  @Override
  public T getActualValue() {
    return actualValue;
  }

  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    return new HashSet<>();
  }

  @Override
  public Boolean isNullable() {
    return values
      .stream()
      .filter(Objects::isNull)
      .map(v -> true)
      .findFirst()
      .orElse(false);
  }


  @Override
  public long getCount() {
    return Long.MAX_VALUE;
  }

  @Override
  public void reset() {
    // Nothing to do
  }


}
