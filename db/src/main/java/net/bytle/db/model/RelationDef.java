package net.bytle.db.model;

import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.DataPath;

import java.util.List;
import java.util.Map;

/**
 *
 * The relational structure of the data
 *
 */
public interface RelationDef {


    /**
     *
     * @param columnName
     * @return a column def by its name
     */
    <T> ColumnDef<T> getColumnDef(String columnName);

    /**
     *
     * @param columnIndex
     * @return a column def by its index
     */
    <T> ColumnDef<T> getColumnDef(Integer columnIndex);

    /**
     *
     * @param columnName
     * @param clazz - The type of the column (Java needs the type to be a sort of type safe)
     * @return  a new columnDef
     */
    <T> ColumnDef<T> getColumnOf(String columnName, Class<T> clazz);

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

  abstract DataPathAbs getDataPath();


  /**
   *
   * @return the number of columns
   *
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
}
