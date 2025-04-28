package com.tabulify.gen.generator;


import com.tabulify.gen.DataGenAttribute;
import com.tabulify.gen.DataGenType;
import com.tabulify.gen.GenColumnDef;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.NoColumnException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Get the values of an other column (called foreign)
 * that has values to return its values randomly
 *
 * @param <T>
 */
public class ForeignColumnGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {


  private Object value;
  private final CollectionGenerator<T> generator;

  /**
   * A generator that generates values that are present in an other columns.
   *
   * @param foreignColumnDef - a {@link ColumnDef column} or a {@link GenColumnDef Gen column}
   */
  public ForeignColumnGenerator(Class<T> clazz, ColumnDef foreignColumnDef) {

    super(clazz);

    if (!(foreignColumnDef instanceof GenColumnDef)) {

      /**
       * This is an external column
       * without data definition,
       * we fetch the data and create an uniform histogram
       */
      this.generator = fetchValuesAndCreateHistogram(clazz, foreignColumnDef);

    } else {

      /**
       * This is a data generation column definition
       * We know then the data definition
       * We don't need to fetch the values
       */
      GenColumnDef genForeignColumn = (GenColumnDef) foreignColumnDef;
      CollectionGenerator<T> generator = genForeignColumn.getOrCreateGenerator(clazz);

      assert generator != null : "A data generator was not found on the column (" + genForeignColumn + ")";
      assert generator.getClass() == SequenceGenerator.class : "The generator of the column (" + genForeignColumn + ") is not a sequence but (" + generator.getClass().getSimpleName() + "). Other generator than a sequence for a primary column are not yet supported";

      // Create the random distribution generator from the sequence
      SequenceGenerator<T> sequenceGenerator = (SequenceGenerator<T>) generator;
      long size = sequenceGenerator
        .getColumnDef()
        .getRelationDef()
        .getDataPath()
        .getSize();
      Object domainMin = sequenceGenerator.getDomainMin(size);
      Object domainMax = sequenceGenerator.getDomainMax(size);
      this.generator = new RandomGenerator<>(clazz, domainMin, domainMax);

    }

  }

  public static <T> ForeignColumnGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {
    /**
     * Properties
     */
    String dataUri = (String) genColumnDef.getDataGeneratorValue(DataGenAttribute.DATA_URI);
    if (dataUri == null) {
      throw new IllegalStateException("The `dataUri` property is mandatory to create a foreign column generator for the column (" + genColumnDef + ") and was not found.");
    }
    String column = (String) genColumnDef.getDataGeneratorValue(DataGenAttribute.COLUMN);
    if (column == null) {
      throw new IllegalStateException("The `column` property is mandatory to create a foreign column generator for the column (" + genColumnDef + ") and was not found.");
    }

    /**
     * Processing
     */
    DataPath dataResource = genColumnDef.getRelationDef().getDataPath().getConnection().getTabular().getDataPath(dataUri);
    if (dataResource == null) {
      throw new IllegalStateException("The data resource defined by the data uri (" + dataUri + ") to create the foreign column generator for the column (" + genColumnDef + ") was not found.");
    }

    ColumnDef foreignColumnDef;
    try {
      foreignColumnDef = dataResource.getOrCreateRelationDef().getColumnDef(column);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The column (" + column + ") was not found on the data resource (" + dataResource + "). We can't create a foreign column generator for the column (" + genColumnDef + ").");
    }
    return (ForeignColumnGenerator<T>) (new ForeignColumnGenerator<>(clazz, foreignColumnDef))
      .setColumnDef(genColumnDef);
  }

  /**
   * Fetch the values
   *
   * @param aClass the class of value to generate
   * @param foreignColumnDef the target foreign column
   * @return the generator
   */
  private HistogramGenerator<T> fetchValuesAndCreateHistogram(Class<T> aClass, ColumnDef foreignColumnDef) {
    // Building the map of value
    try (
      SelectStream selectStream = foreignColumnDef.getRelationDef().getDataPath().getSelectStream()
    ) {
      Map<T, Double> histogram = new HashMap<>();
      while (selectStream.next()) {
        histogram.put(selectStream.getObject(foreignColumnDef.getColumnName(), aClass), 1.0);
      }
      if (histogram.isEmpty()) {
        throw new RuntimeException("The foreign table (" + foreignColumnDef.getRelationDef().getDataPath().toString() + ") has no data for the column (" + foreignColumnDef + ")");
      }
      return HistogramGenerator.create(aClass, histogram);
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    value = generator.getNewValue();
    //noinspection unchecked
    return (T) value;

  }

  /**
   * @return the actual value
   */
  @Override
  public T getActualValue() {
    return clazz.cast(value);
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
    generator.reset();
  }


  /**
   * @return the parent generator of this foreign column generator
   */
  public CollectionGenerator<?> getGenerator() {
    return this.generator;
  }

  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.FOREIGN_COLUMN;
  }

}
