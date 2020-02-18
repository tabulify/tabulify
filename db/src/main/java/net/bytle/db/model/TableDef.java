package net.bytle.db.model;


import net.bytle.db.engine.Columns;
import net.bytle.db.spi.DataPathAbs;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.*;

/**
 *
 * The default DataDef implementation
 *
 * ColumnDef is a generic and we may extend them
 * Therefore all column function are below separated
 * (
 * ie you can't cast directly a generic with a parameter, by isolating them, it's possible to create
 * a get list for each columnDef extension (ie DataGenColumnDef for instance)
 *
 */
public class TableDef extends DataDefAbs  {


  private Map<String, ColumnDef> columnDefByName = new HashMap<>();

  public TableDef(DataPathAbs dataPath) {
    super(dataPath);
  }


  public static TableDef of(DataPathAbs dataPath) {
    return new TableDef(dataPath);
  }

  public TableDef addColumn(String columnName) {
    addColumn(columnName, null, null, null, null, null);
    return this;
  }

  public TableDef addColumn(String columnName, Integer typeCode) {
    addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public TableDef addColumn(String columnName, Integer type, Integer precision) {
    addColumn(columnName, type, precision, null, null, null);
    return this;
  }

  public TableDef addColumn(String columnName, Integer type, Boolean nullable) {
    addColumn(columnName, type, null, null, nullable, null);
    return this;
  }

  public TableDef addColumn(String columnName, Integer type, Integer precision, Integer scale) {
    addColumn(columnName, type, precision, scale, null, null);
    return this;
  }

  public TableDef addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable) {
    addColumn(columnName, type, precision, scale, nullable, null);
    return this;
  }

  public TableDef addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable, String comment) {
    int columnNullable;

    if (nullable == null) {
      columnNullable = DatabaseMetaData.columnNullableUnknown;
    } else if (!nullable) {
      columnNullable = DatabaseMetaData.columnNullable;
    } else {
      columnNullable = DatabaseMetaData.columnNoNulls;
    }

    if (type == null) {
      type = Types.VARCHAR;
    }

    SqlDataType sqlDataType = this.getDataPath().getDataStore().getSqlDataType(type);
    if (sqlDataType==null){
      throw new RuntimeException("The data store ("+this.getDataPath().getDataStore()+") does not know the numeric type code ("+type+")");
    }

    Class<?> clazz = sqlDataType.getClazz();
    if (clazz==null){
      throw new RuntimeException("The sql data type ("+sqlDataType.getTypeName()+") of the data store ("+this.getDataPath().getDataStore()+") does not have any class associated to it");
    }
    getColumnOf(columnName, clazz)
      .typeCode(type)
      .precision(precision)
      .scale(scale)
      .setNullable(columnNullable)
      .comment(comment);
    return this;
  }

  public TableDef addColumn(String columnName, Integer type, Integer precision, Boolean nullable) {
    addColumn(columnName, type, precision, null, nullable, null);
    return this;
  }



  /**
   * Return the columns by position
   *
   * @return
   */
  public ColumnDef[] getColumnDefs() {

    List<ColumnDef> columnDefs = new ArrayList<>(columnDefByName.values());
    columnDefs.sort(
      (Comparator.comparing(ColumnDef::getColumnPosition))
    );
    return columnDefs.toArray(new ColumnDef[0]);

  }


  /**
   * @param columnName
   * @return the column or null if not found
   */
  public <T> ColumnDef<T> getColumnDef(String columnName) {

    return (ColumnDef<T>) columnDefByName.get(columnName);
  }

  /**
   * @param columnIndex
   * @return a columnDef by index starting at 0
   */
  public <T> ColumnDef<T> getColumnDef(Integer columnIndex) {

    //noinspection unchecked
    return this.columnDefByName
      .values()
      .stream()
      .filter(c-> c.getColumnPosition().equals(columnIndex+1))
      .findFirst()
      .orElse(null);

  }


  /**
   * @param columnName - The column name
   * @param clazz      - The type of the column (Java needs the type to be a sort of type safe)
   * @return a new columnDef
   */
  public <T> ColumnDef<T> getColumnOf(String columnName, Class<T> clazz) {

    ColumnDef<T> columnDef;
    ColumnDef columnDefGet = getColumnDef(columnName);
    if (columnDefGet == null) {

      // This assert is to catch when object are passed
      // to string function, the length is bigger than the assertion and make it fails
      assert columnName.length() < 100;
      columnDef = new ColumnDef<>(this, columnName, clazz);
      columnDef.setColumnPosition(columnDefByName.size() + 1);
      columnDefByName.put(columnName, columnDef);
    } else {
      columnDef = Columns.safeCast(columnDefGet, clazz);
    }
    return columnDef;

  }

  @Override
  public int getColumnsSize() {
    return columnDefByName.size();
  }

  /**
   * @param columnNames
   * @return an array of columns
   * The columns must exist otherwise you of a exception
   */
  protected ColumnDef[] getColumns(String... columnNames) {

    List<ColumnDef> columnDefs = new ArrayList<>();
    for (String columnName : columnNames) {
      final ColumnDef column = getColumnDef(columnName);
      if (column != null) {
        columnDefs.add(column);
      } else {
        throw new RuntimeException("The column (" + columnName + ") was not found for the table (" + this + ")");
      }
    }
    return columnDefs.toArray(new ColumnDef[columnDefs.size()]);
  }

  /**
   * @param columnNames
   * @return an array of columns
   * The columns must exist otherwise you of a exception
   */
  protected ColumnDef[] getColumns(List<String> columnNames) {

    return getColumns(columnNames.toArray(new String[0]));
  }

  @Override
  public String toString() {
    return "DataDef of " + dataPath;
  }

  protected void addColumnDef(ColumnDef columnDef) {
    columnDefByName.put(columnDef.getColumnName(),columnDef);
  }


}
