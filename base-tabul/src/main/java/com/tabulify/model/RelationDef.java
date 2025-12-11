package com.tabulify.model;

import com.tabulify.diff.DataPathDiffResult;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.NoColumnException;
import com.tabulify.type.KeyInterface;
import com.tabulify.type.KeyNormalizer;

import java.util.List;
import java.util.Map;

/**
 * Represents a relational structure
 * <p>
 * In the merge and copy function, the below term are used:
 * * A `relation` is a combination of the columns and the local constraints (Unique keys and Primary Key)
 * * A `relationWithForeignKey` is the whole object (ie a `relation` and the foreign Keys)
 * * A `relationWithoutLocalConstraint` is the columns only
 * <p>
 * For merge, copy, the argument should be a relation def because of all executable, the relation def structure
 * is known after execution at {@link SelectStream#getRuntimeRelationDef()}
 * <p>
 *
 * @see <a href=https://calcite.apache.org/docs/model.html#table>Calcite table</a>
 */
public interface RelationDef {


  /**
   * @param columnName the column name
   * @return a column def by its name or null if it does not exist
   */
  ColumnDef<?> getColumnDef(String columnName) throws NoColumnException;

  /**
   * @param columnIndex the column index
   * @return a column def by its index (starting at 0)
   * @throws IllegalArgumentException if the column was not found
   */
  ColumnDef<?> getColumnDef(Integer columnIndex);

  /**
   * Get or create column
   * <p>
   * This is the unique factory method that create the column object
   * All other add method call this method
   *
   * @param columnName  the column name
   * @param sqlDataType the type
   */
  <T> ColumnDef<T> getOrCreateColumn(String columnName, SqlDataType<T> sqlDataType);


  /**
   * @param columnName - The column name
   * @return a new columnDef even if the column already existed
   */
  <T> ColumnDef<T> createColumn(String columnName, SqlDataType<T> sqlDataType);

  /**
   * An utility function that create a column from a Java Clazz
   * This is to implement a sort of type control mostly on generation function
   */
  <T> ColumnDef<T> getOrCreateColumn(String columnName, Class<T> clazz);


  PrimaryKeyDef getPrimaryKey();

  List<ForeignKeyDef> getForeignKeys();

  ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKeyDef, String... columnNames);

  List<UniqueKeyDef> getUniqueKeys();

  RelationDef setPrimaryKey(String... columnNames);

  RelationDef addUniqueKey(String... columnNames);

  RelationDef addForeignKey(PrimaryKeyDef primaryKeyDef, String... columnNames);

  RelationDef addForeignKey(PrimaryKeyDef primaryKeyDef, List<String> columnNames);

  RelationDef addForeignKey(DataPath dataPath, String... columnNames);

  RelationDef setPrimaryKey(List<String> columnNames);

  void deleteForeignKey(ForeignKeyDef foreignKeyDef);

  ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKey, List<String> columns);

  PrimaryKeyDef primaryKeyOf(String... columnNames);

  /**
   * @return the number of columns
   */
  int getColumnsSize();

  RelationDef addColumn(String columnName);

  RelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode);


  RelationDef addColumn(String columnName, Class<?> clazz);

  RelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode, Boolean nullable);

  RelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode, int precision);

  RelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode, int precision, Boolean nullable);

  RelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode, int precision, int scale);

  RelationDef addColumn(String columnName, SqlDataTypeAnsi typeCode, int precision, int scale, Boolean nullable, String comment);

  /**
   * The column definition by their position
   */
  <D extends ColumnDef<?>> List<D> getColumnDefs();


  /**
   * Drop all.
   * Drop all meta columns and constraint
   * This function is used principally in test
   * to be sure that there is no columns that comes from the backend metadata store
   * You may also use {@link DataPath#createEmptyRelationDef()}
   */
  RelationDef dropAll();

  RelationDef removeMetadataPrimaryKey();

  RelationDef removeMetadataUniqueKey(UniqueKeyDef uniqueKeyDef);

  RelationDef removeAllMetadataUniqueKeys();

  DataPathDiffResult diff(DataPath target);

  /**
   * The data path
   */
  DataPath getDataPath();

  /**
   * Merge the columns definition
   * A relation def and not a data path because
   * the final relation def of executable is known only at runtime
   * (ie {@link SelectStream#getRuntimeRelationDef()}
   */
  RelationDef mergeColumns(RelationDef relationDef);

  /**
   * Copy columns and constraints (struct)
   */
  RelationDefAbs copyStruct(DataPath sourceDataPath);

  RelationDef copyPrimaryKeyFrom(RelationDef relationDef);

  /**
   * Merge all local constraints (ie all constraint except the foreign key)
   */
  RelationDef mergeLocalConstraints(RelationDef relationDef);

  RelationDef copyUniqueKeysFrom(RelationDef relationDef);

  /**
   * Merge the data def (ie struct (cols + constraints) + foreign keys)
   *
   * @param sourceDataPath - the data path to merge from (the source of the meta)
   * @param sourceTargets  - an optional mapping of source/target in case of cross system
   *                       (ie if the target and the source does not have the same name)
   *                       It happens with transfer
   *                       * between oracle (uppercase by default)
   *                       * or with templating (Glob Backreference $0 or attribute ${name})
   *                       and any other database
   *                       * or with a system without store (ie memory)
   *                       Example: d_cat in the source would become D_CAT in Oracle
   */
  <D1 extends DataPath, D2 extends DataPath, D3 extends DataPath> RelationDef mergeDataDef(D1 sourceDataPath, Map<D2, D3> sourceTargets);

  /**
   * Merge the data def (ie struct + foreign keys)
   * Utility Call {@link #mergeDataDef(DataPath, Map)}
   */
  RelationDef mergeDataDef(DataPath fromDataPath);

  /**
   * @param targetDataPath - the data path to copy the foreign key from (ie the target)
   * @param targetSources  - an optional mapping of target source in case of cross system metadata copy
   *                       (ie if the target and the source does not have the same name)
   *                       It happens with transfer between oracle (uppercase by default) and any other database
   *                       Example: d_cat in the source would become D_CAT in Oracle
   */
  <D1 extends DataPath, D2 extends DataPath, D3 extends DataPath> RelationDef copyForeignKeysFrom(D1 targetDataPath, Map<D2, D3> targetSources);

  /**
   * Copy the foreign keys
   */
  RelationDef copyForeignKeysFrom(DataPath sourceDataPath);

  /**
   * Merge struct (columns + local constraints, no foreign keys)
   * To merge also the foreign keys, see {@link #mergeDataDef(DataPath, Map)}
   * The argument is a relation def because for all executable, the relation def structure
   * is known after execution at {@link SelectStream#getRuntimeRelationDef()}
   */
  RelationDef mergeStruct(RelationDef relationDef);

  /**
   * Copy the data def (struct (columns + constraints) and foreign keys)
   * To copy also the foreign keys, see {@link #copyDataDef(DataPath, Map)}
   */
  RelationDef copyDataDef(DataPath fromDataPath);

  /**
   * Copy the data def (struct (columns + constraints) and foreign keys)
   *
   * @param sourceDataPath  - the source of the meta
   * @param sourceTargetMap - the source target map
   */
  RelationDef copyDataDef(DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargetMap);

  /**
   * Get the columns list in a relational data path format
   */
  DataPath toColumnsDataPathBy(ColumnAttribute columnOrder, ColumnAttribute... columnAttributes);


  boolean hasColumn(String columnName);

  boolean hasColumn(KeyNormalizer columnName);


  RelationDef setPrimaryKey(ColumnDef<?>... primaryColumn);


  ColumnDef<?> getColumnDef(KeyNormalizer name);


  RelationDef addColumn(String columnName, SqlDataType<?> dataType);


  RelationDef addColumn(String columnName, KeyNormalizer typeName);

  /**
   * Function added to type check the column class returned
   * (ie the column with the name columnName should have the same clazz)
   */
  <T> ColumnDef<T> getColumnDef(String columnName, Class<T> clazz);

  RelationDef addColumn(String columnName, SqlDataTypeKeyInterface typeKeyInterface);

  RelationDef addColumn(String columnName, KeyInterface typeName);

  RelationDef addColumn(String columnName, SqlDataType<?> sqlDataType, int precision, int scale, Boolean nullable, String comment);

  ColumnDef<?> getColumnDefSafe(String columnName);
}
