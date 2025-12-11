package com.tabulify.gen;

import com.tabulify.gen.generator.CollectionGenerator;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.exception.NoColumnException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenRelationDef extends RelationDefDefault {


  /**
   * @param DataPath may be {@link GenDataPath} or {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
   */
  public <T extends DataPath> GenRelationDef(T DataPath) {
    super(DataPath);
  }


  public GenRelationDef addColumn(String columnName) {
    this.addColumn(columnName, DEFAULT_DATA_TYPE, 0, 0, null, null);
    return this;
  }

  @Override
  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode) {
    this.addColumn(columnName, typeCode, 0, 0, null, null);
    return this;
  }

  @Override
  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi type, int precision) {
    this.addColumn(columnName, type, precision, 0, null, null);
    return this;
  }

  @Override
  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi type, Boolean nullable) {
    this.addColumn(columnName, type, 0, 0, nullable, null);
    return this;
  }

  @Override
  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi type, int precision, int scale) {
    this.addColumn(columnName, type, precision, scale, null, null);
    return this;
  }

  @Override
  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi type, int precision, int scale, Boolean nullable) {
    this.addColumn(columnName, type, precision, scale, nullable, null);
    return this;
  }


  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi type, int precision, int scale, Boolean nullable, String comment) {


    SqlDataType<?> dataType = this.getDataPath().getConnection().getSqlDataType(type);
    if (this.hasColumn(columnName)) {
      GenLog.LOGGER.warning("The column (" + columnName + ") was already defined, you can't add it");
      return this;
    }

    createColumn(columnName, dataType)
      .setPrecision(precision)
      .setScale(scale)
      .setNullable(nullable)
      .setComment(comment);

    return this;

  }

  @Override
  public <T> GenColumnDef<T> createColumn(String columnName, SqlDataType<T> sqlDataType) {


    // This assert is to catch when object are passed
    // to string function, the length is bigger than the assertion and make it fails
    assert columnName.length() < 100;
    GenColumnDef<T> columnDef = GenColumnDef.createOf(this, columnName, sqlDataType);

    ColumnDef<?> oldColumn = columnDefByName.get(columnDef.getColumnNameNormalized());
    if (oldColumn == null) {
      /**
       * It's important to use the function {@link #getColumnsSize()}
       * and not {@link columnDefByName}
       * to get the size because columns may be hidden
       * {@link GenColumnDef#setIsHidden(boolean)}
       * and therefore does not count
       */
      int size = getColumnsSize();
      columnDef.setColumnPosition(size + 1);
    } else {
      columnDef.setColumnPosition(oldColumn.getColumnPosition());
    }
    super.columnDefByName.put(columnDef.getColumnNameNormalized(), columnDef);

    return columnDef;

  }

  @Override
  public List<GenColumnDef<?>> getColumnDefs() {
    return super.getColumnDefs()
      .stream()
      .map(e -> (GenColumnDef<?>) e)
      .filter(GenColumnDef::isNotHidden)
      .sorted()
      .collect(Collectors.toList());
  }


  @Override
  public GenRelationDef addColumn(String columnName, SqlDataTypeAnsi type, int precision, Boolean nullable) {
    this.addColumn(columnName, type, precision, 0, nullable, null);
    return this;
  }


  /**
   * Utility function to reset the data def
   * TODO: we may use the {@link DataPath#createRelationDef()} instead
   *
   * @return the relation
   */
  @Override
  public GenRelationDef dropAll() {
    super.dropAll();
    return this;
  }


  @Override
  public GenColumnDef<?> getColumnDef(String columnName) throws NoColumnException {
    return (GenColumnDef<?>) super.getColumnDef(columnName);
  }

  @Override
  public <T> GenColumnDef<T> getOrCreateColumn(String columnName, Class<T> clazz) {

    return (GenColumnDef<T>) super.getOrCreateColumn(columnName, clazz);

  }

  @Override
  public GenColumnDef<?> getColumnDef(Integer columnIndex) {
    return (GenColumnDef<?>) super.getColumnDef(columnIndex);
  }

  @Override
  public <T> GenColumnDef<T> getOrCreateColumn(String columnName, SqlDataType<T> sqlDataType) {

    if (this.hasColumn(columnName)) {
      throw new RuntimeException("The column (" + columnName + ") is already defined");
    }

    return createColumn(columnName, sqlDataType);

  }


  /**
   * We do this for the {@link GenColumnDef#setIsHidden(boolean) hidden column feature }
   */
  @Override
  public int getColumnsSize() {
    return getColumnDefs().size();
  }

  /**
   * We do this for the {@link GenColumnDef#setIsHidden(boolean) hidden column feature }
   */
  public List<? extends GenColumnDef<?>> getAllColumnDefs() {

    //noinspection unchecked
    return (List<? extends GenColumnDef<?>>) super.getColumnDefs();

  }

  @Override
  public GenRelationDef copyDataDef(DataPath fromDataPath) {
    return (GenRelationDef) super.copyDataDef(fromDataPath);
  }

  @Override
  public GenColumnDef<String> getOrCreateColumn(String columnName) {
    return (GenColumnDef<String>) super.getOrCreateColumn(columnName);
  }


  /**
   * @param collectionGenerators - collection of generators that comes from {@link #buildGeneratorInCreateOrder()}
   *                             and may be cached
   * @return a row for all DataPath that uses GenRelationDef
   * ie {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
   * and {@link GenDataPath}
   */
  public Map<ColumnDef<?>, Object> buildRowFromGenerators(List<CollectionGenerator<?>> collectionGenerators) {
    Map<ColumnDef<?>, Object> row = new HashMap<>();
    for (CollectionGenerator<?> c : collectionGenerators) {
      GenColumnDef<?> columnDef = c.getColumnDef();
      Object newValue = c.getNewValue();
      row.put(columnDef, newValue);
    }
    return row;

  }

  /**
   * Build/Rebuild the generator and return them in create order
   * (Generator may have parent and therefore the parent value
   * should be generated first)
   *
   * @return the generator in create order
   * (No data path builder proof as this should have been created in a builder)
   */
  public List<CollectionGenerator<?>> buildGeneratorInCreateOrder() {

    /**
     * Hack: We delete the generators
     * Why? They are for now created recursively by each column if the
     * parent generator of a column does not exist
     * Not best but yeah
     */
    for (GenColumnDef<?> columnDef : this.getColumnDefs()) {
      Object value = columnDef.getVariable(GenColumnAttribute.DATA_SUPPLIER).getValueOrNull();
      if (value == null) {
        // No data supplier metadata, we can't recreate the
        // generator, we don't delete it then
        continue;
      }
      columnDef.deleteGenerator();
    }

    List<? extends CollectionGenerator<?>> generators = this
      .getAllColumnDefs()
      .stream()
      .map(c -> {
          /**
           * Flaw Generator may be created recursively by each column
           * ie a column that does not have a parent will create it
           * ie {@link GenColumnDef#createGenerator(Class)} may return null
           */
        return (CollectionGenerator<?>) c.getOrCreateGenerator();
        }
      )
      .collect(Collectors.toList());

    return CollectionGenerator
      .createDag()
      .addRelations(generators)
      .getCreateOrdered();


  }

  public <T> GenColumnDef<T> getColumnDef(String columnName, Class<T> clazz) {
    return (GenColumnDef<T>) super.getColumnDef(columnName, clazz);
  }

  @Override
  public DataPath getDataPath() {
    /**
     * Maybe a {@link GenDataPath} or an {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
     */
    return super.getDataPath();
  }
}
