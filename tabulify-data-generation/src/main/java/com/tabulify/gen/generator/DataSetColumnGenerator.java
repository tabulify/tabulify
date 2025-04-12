package com.tabulify.gen.generator;

import com.tabulify.gen.GenColumnDef;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Casts;

import java.util.Collections;
import java.util.Set;

/**
 * A data generator that takes a row from a data set
 * and return a specified column
 *
 * @param <T>
 */
public class DataSetColumnGenerator<T> extends CollectionGeneratorAbs<T> {

  private final DataSetGenerator<?> dataSetGenerator;
  private final ColumnDef entityColumn;
  private T actualValue;

  /**
   * @param clazz                  - the clazz
   * @param entityColumnName       - the column name in the data set
   * @param parentDataSetGenerator - the parent entity generator
   */
  public DataSetColumnGenerator(Class<T> clazz, String entityColumnName, DataSetGenerator<?> parentDataSetGenerator) {
    super(clazz);
    this.dataSetGenerator = parentDataSetGenerator;
    DataPath entity = parentDataSetGenerator.getEntity();
    try {
      this.entityColumn = entity.getOrCreateRelationDef().getColumnDef(entityColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The entity column named (" + entityColumnName + ") was not found in the entity resource (" + entity + ")");
    }
  }

  public static <T> DataSetColumnGenerator<T> create(Class<T> clazz, String entityColumnName, DataSetGenerator<?> parentDataSetGenerator) {
    return new DataSetColumnGenerator<>(clazz, entityColumnName, parentDataSetGenerator);
  }

  /**
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   *
   */
  public static <T> DataSetColumnGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {
    /**
     * The column
     */
    String entityColumnName = genColumnDef.getGeneratorProperty(String.class, "column");
    if (entityColumnName == null) {
      throw new IllegalStateException("The generator definition of the column (" + genColumnDef + ") miss the `column` property. This property defines the column that will be used to return the data and is therefore mandatory.");
    }
    /**
     * Try to find the parent generator
     */
    String parentKey = "parent";
    String columnParent = genColumnDef.getGeneratorProperty(String.class, parentKey);

    if (columnParent != null) {
      GenColumnDef parentColumn = genColumnDef.getRelationDef().getColumnDefs()
        .stream()
        .filter(c -> c.getColumnName().equals(columnParent))
        .findFirst()
        .orElse(null);
      if (parentColumn == null) {
        throw new IllegalStateException("The " + parentKey + " attribute of the column " + genColumnDef + " defines a column (" + columnParent + ") that does not exists");
      }
      CollectionGenerator<?> collectionGenerator = parentColumn.getOrCreateGenerator(clazz);
      if (!(collectionGenerator instanceof DataSetGenerator)) {
        throw new IllegalStateException("The column parent " + columnParent + " defines in the generation of the column " + genColumnDef + " has a generator that is not a `dataset` generator but " + collectionGenerator.getGeneratorType());
      } else {
        return new DataSetColumnGenerator<>(clazz, entityColumnName, (DataSetGenerator<?>) collectionGenerator);
      }
    } else {
      throw new IllegalStateException("The `" + parentKey + "` attribute is mandatory and was not found in the generation definition of the column " + genColumnDef + ".");
    }

  }

  @Override
  public T getNewValue() {
    Object sourceObject = dataSetGenerator.getActualRow().get(this.entityColumn.getColumnPosition() - 1);
    actualValue = Casts.castSafe(sourceObject, this.clazz);
    return actualValue;
  }

  @Override
  public T getActualValue() {
    return actualValue;
  }

  @Override
  public Set<CollectionGenerator<?>> getDependencies() {

    return Collections.singleton(this.dataSetGenerator);

  }

  @Override
  public long getCount() {
    return this.dataSetGenerator.getCount();
  }

  @Override
  public void reset() {
    // Nothing to do
  }

}
