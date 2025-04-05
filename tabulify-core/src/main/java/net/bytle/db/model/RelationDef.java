package net.bytle.db.model;

import net.bytle.db.diff.DataPathDataComparison;
import net.bytle.db.spi.DataPath;
import net.bytle.exception.NoColumnException;

import java.util.List;

/**
 * Represents a relational structure
 * <p>
 * In the merge and copy function, the below term are used:
 * * A `relation` is a combination of the columns and the local constraints (Unique keys and Primary Key)
 * * A `relationWithForeignKey` is the whole object (ie a `relation` and the foreign Keys)
 * * A `relationWithoutLocalConstraint` is the columns only
 *
 * @see <a href=https://calcite.apache.org/docs/model.html#table>Calcite table</a>
 */
public interface RelationDef {


  /**
   * @param columnName the column name
   * @return a column def by its name or null if it does not exist
   */
  ColumnDef getColumnDef(String columnName) throws NoColumnException;

  /**
   * @param columnIndex the column index
   * @return a column def by its index (starting at 0)
   */
  ColumnDef getColumnDef(Integer columnIndex);

  /**
   * Get or create column
   * <p>
   * This is the unique factory method that create the column object
   * All other add method call this method
   *
   * @param columnName the column name
   * @param clazz       - to have a minimal compile type checking
   * @param sqlDataType the type
   */
  ColumnDef getOrCreateColumn(String columnName, SqlDataType sqlDataType, Class<?> clazz);


  /**
   * @param columnName - The column name
   * @param clazz      - The type of the column (Java needs the type to be a sort of type safe)
   * @return a new columnDef even if the column already existed
   */
  ColumnDef createColumn(String columnName, SqlDataType sqlDataType, Class<?> clazz);

  /**
   * An utility function that create a column from a Java Clazz
   * This is to implement a sort of type control mostly on generation function
   *
   */
  ColumnDef getOrCreateColumn(String columnName, Class<?> clazz);


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

  RelationDef addColumn(String s);

  RelationDef addColumn(String columnName, Integer typeCode);

  RelationDef addColumn(String columnName, Class<?> clazz);

  RelationDef addColumn(String columnName, Integer typeCode, Boolean nullable);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision, Boolean nullable);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision, Integer scale);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision, Integer scale, Boolean nullable, String comment);

  /**
   * The column definition by their position
   *
   */
  <D extends ColumnDef> List<D> getColumnDefs();


  /**
   * Drop all columns
   * This function is used principally in test
   * to be sure that there is no columns that comes from the backend metadata store
   *
   */
  RelationDef dropAll();

  RelationDef removeMetadataPrimaryKey();

  RelationDef removeMetadataUniqueKey(UniqueKeyDef uniqueKeyDef);

  RelationDef removeAllMetadataUniqueKeys();

  DataPathDataComparison compareData(DataPath target);


  /**
   * So much used that we shortcut it
   *
   */
  DataPath getDataPath();

  RelationDef mergeStructWithoutConstraints(DataPath fromDataPath);

  RelationDefAbs copyStruct(DataPath from);

  RelationDef copyPrimaryKeyFrom(DataPath from);

  RelationDef mergeDataDef(DataPath fromDataPath);

  RelationDef copyForeignKeysFrom(DataPath source);

  RelationDef mergeStruct(DataPath fromDataPath);

  RelationDef copyDataDef(DataPath fromDataPath);

  /**
   * Get the columns list in a relational data path format
   *
   */
  DataPath toColumnsDataPathBy(ColumnAttribute columnOrder, ColumnAttribute... columnAttributes);


  boolean hasColumn(String columnName);

}
