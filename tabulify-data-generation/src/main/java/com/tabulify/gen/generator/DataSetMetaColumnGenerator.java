package com.tabulify.gen.generator;

import com.tabulify.gen.DataGenType;
import com.tabulify.gen.GenColumnDef;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Casts;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A data generator that returns a meta column value (ie another column)
 * from a data set generator
 */
public class DataSetMetaColumnGenerator<T> extends CollectionGeneratorAbs<T> {

  private final DataSetGenerator<?> dataSetGenerator;
  private final ColumnDef columnName;
  private T actualValue;

  /**
   * @param clazz                  - the clazz
   * @param columnName             - the column name in the data set
   * @param parentDataSetGenerator - the parent entity generator
   */
  public DataSetMetaColumnGenerator(Class<T> clazz, String columnName, DataSetGenerator<?> parentDataSetGenerator) {
    super(clazz);
    this.dataSetGenerator = parentDataSetGenerator;
    DataPath dataSetPath = parentDataSetGenerator.getDataSet();
    try {
      this.columnName = dataSetPath.getOrCreateRelationDef().getColumnDef(columnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The column named (" + columnName + ") was not found in the data set resource (" + dataSetPath + ")");
    }
  }

  public static <T> DataSetMetaColumnGenerator<T> create(Class<T> clazz, String entityColumnName, DataSetGenerator<?> parentDataSetGenerator) {
    return new DataSetMetaColumnGenerator<>(clazz, entityColumnName, parentDataSetGenerator);
  }

  /**
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   */
  public static <T> DataSetMetaColumnGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {

    Map<DataSetMetaArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(DataSetMetaArgument.class);
    /**
     * The column
     */
    String columnName = (String) argumentMap.get(DataSetMetaArgument.COLUMN);
    if (columnName == null) {
      columnName = genColumnDef.getColumnName();
    }

    /**
     * Try to find the parent generator
     */
    DataSetMetaArgument dataSetColumnAttribute = DataSetMetaArgument.COLUMN_DATA_SET;
    String columnParent = (String) argumentMap.get(dataSetColumnAttribute);

    if (columnParent == null) {
      throw new IllegalStateException("The `" + dataSetColumnAttribute + "` attribute is mandatory and was not found in the generation definition of the column " + genColumnDef + ".");
    }
    GenColumnDef<?> parentColumn = genColumnDef.getGenRelationDef().getColumnDefs()
      .stream()
      .filter(c -> c.getColumnName().equals(columnParent))
      .findFirst()
      .orElse(null);

    if (parentColumn == null) {
      throw new IllegalStateException("The " + dataSetColumnAttribute + " attribute of the column " + genColumnDef + " defines a column (" + columnParent + ") that does not exists");
    }

    CollectionGenerator<?> collectionGenerator = parentColumn.getOrCreateGenerator();
    if (!(collectionGenerator instanceof DataSetGenerator)) {
      throw new IllegalStateException("The data set column " + columnParent + " defines in the generation of the column " + genColumnDef + " has a generator that is not a `dataset` generator but " + collectionGenerator.getGeneratorType());
    }

    return new DataSetMetaColumnGenerator<>(clazz, columnName, (DataSetGenerator<?>) collectionGenerator);


  }

  @Override
  public T getNewValue() {
    Object sourceObject = dataSetGenerator.getActualRow().get(this.columnName.getColumnPosition() - 1);
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

  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.DATA_SET_META;
  }

  @Override
  public Boolean isNullable() {
    return columnName.isNullable();
  }

}
