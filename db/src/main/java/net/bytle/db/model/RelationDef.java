package net.bytle.db.model;

import net.bytle.db.spi.DataPath;

import java.util.List;
import java.util.Map;

/**
 * The relational structure of the data
 */
public interface RelationDef {


  /**
   * @param columnName
   * @return a column def by its name
   */
  <T> ColumnDef<T> getColumnDef(String columnName);

  /**
   * @param columnIndex
   * @return a column def by its index (starting at 0)
   */
  <T> ColumnDef<T> getColumnDef(Integer columnIndex);

  /**
   * Get or create column
   *
   * This is the unique factory method that create the column object
   * All other add method call this method
   *
   * @param columnName
   * @param clazz
   * @param <T>
   * @return
   */
  <T> ColumnDef<T> getOrCreateColumn(String columnName, Class<T> clazz);

  PrimaryKeyDef getPrimaryKey();

  List<ForeignKeyDef> getForeignKeys();

  ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKeyDef, String... columnNames);

  List<UniqueKeyDef> getUniqueKeys();

  RelationDef setPrimaryKey(String... columnNames);

  RelationDef addUniqueKey(String name, String... columnNames);

  RelationDef addForeignKey(PrimaryKeyDef primaryKeyDef, String... columnNames);

  RelationDef addForeignKey(PrimaryKeyDef primaryKeyDef, List<String> columnNames);

  RelationDef addForeignKey(DataPath dataPath, String... columnNames);

  RelationDef setPrimaryKey(List<String> columnNames);

  void deleteForeignKey(ForeignKeyDef foreignKeyDef);

  Object getProperty(String key);

  DataDefAbs addProperty(String key, Object value);

  Map<String, Object> getProperties();

  ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKey, List<String> columns);

  PrimaryKeyDef primaryKeyOf(String... columnNames);

  DataPath getDataPath();


  /**
   * @return the number of columns
   */
  int getColumnsSize();

  @Override
  String toString();

  RelationDef addColumn(String s);

  RelationDef addColumn(String columnName, Integer typeCode);

  RelationDef addColumn(String columnName, Integer typeCode, Boolean nullable);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision, Boolean nullable);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision, Integer scale);

  RelationDef addColumn(String columnName, Integer typeCode, Integer precision, Integer scale, Boolean nullable, String comment);

  ColumnDef[] getColumnDefs();

  /**
   * Copy the data definitions (columns,..)
   *
   * @param sourceDataPath
   * @return
   */
  RelationDef copyDataDef(DataPath sourceDataPath);

  /**
   *
   * @param columnName
   * @param clazz
   * @param <T>
   * @return the column checked against the clazz
   * This is an utility function that permits to have type safe statement
   */
  <T> ColumnDef<T> getColumn(String columnName, Class<T> clazz);

  /**
   *
   * @param columnName
   * @return a column def or null if the column does not exist
   */
  ColumnDef getColumn(String columnName);

  DataDefAbs mergeDataDef(DataPath fromDataPath);
}
