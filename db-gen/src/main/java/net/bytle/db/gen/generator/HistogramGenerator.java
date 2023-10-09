package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;
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
 * * Probabilities sum does not have to equal one, they will be be normalize to make them sum to one.
 * http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
 * <p>
 * See another example at
 * https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/distribution/EnumeratedDistribution.html
 */
public class HistogramGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {


  public static final String TYPE = "histogram";


  private T actualValue;

  private List<Object> values;

  /**
   * @param buckets - the buckets where the data needs to be generated
   * @param clazz     - the return clazz of the value - this is used from the {@link DataSetGenerator} where
   *                  an histogram of the row (long) is used to choose the value
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
    return new HistogramGenerator<T>(clazz, buckets);
  }

  /**
   * Instantiate an expression generator from the columns properties
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   * @param genColumnDef
   * @param <T>
   * @return
   */
  public static <T> HistogramGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef){

    Object objectBucket = genColumnDef.getGeneratorProperty(Object.class, "buckets");
    if (objectBucket==null){
      throw new RuntimeException("The buckets column property is mandatory and was not found for the column ("+genColumnDef+")");
    }
    Map<T, Double> buckets;
    if (objectBucket instanceof Map) {
      buckets = genColumnDef.getGeneratorMapProperty(clazz, Double.class,"buckets");
    } else if (objectBucket instanceof Collection){
      buckets = Casts.castToListSafe(objectBucket, clazz)
        .stream()
        .collect(Collectors.toMap(
          s->s,
          s->1.0
        ));
    } else {
      throw new RuntimeException("The `buckets` value ("+objectBucket+") of the column ("+genColumnDef+") are not a list or a map");
    }
    return new HistogramGenerator<T>(clazz, buckets);

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
  public long getCount() {
    return Long.MAX_VALUE;
  }

  @Override
  public void reset() {
    // Nothing to do
  }


}
