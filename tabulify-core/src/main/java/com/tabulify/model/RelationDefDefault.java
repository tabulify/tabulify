package com.tabulify.model;


import com.tabulify.spi.DataPath;
import net.bytle.exception.NoColumnException;
import net.bytle.type.MapKeyIndependent;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ColumnDef can be extended therefore all column function are below separated
 * <p>
 * Why ?
 * Because:
 * * {@link ColumnDefBase columnDef} is a generic data type with the class
 * * you can't cast directly a generic with a parameter
 * <p>
 * By isolating them in a different class, it's possible to create
 * a get list for each columnDef extension (ie DataGenColumnDef for instance)
 */
public class RelationDefDefault extends RelationDefAbs {


  /**
   * But Oracle by default put all name in uppercase when quoting is disabled
   * we should apply the same transform but RelationDef is system independent
   * so we make it case independent here
   */
  protected MapKeyIndependent<ColumnDef> columnDefByName = new MapKeyIndependent<>();

  public <T extends DataPath> RelationDefDefault(T DataPath) {
    super(DataPath);
  }


  public RelationDefDefault addColumn(String columnName) {
    addColumn(columnName, Types.VARCHAR, null, null, null, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer typeCode) {
    addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Class<?> clazz) {
    SqlDataType typeCode = this.getDataPath().getConnection().getSqlDataType(clazz);
    addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer type, Integer precision) {
    addColumn(columnName, type, precision, null, null, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer type, Boolean nullable) {
    addColumn(columnName, type, null, null, nullable, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer type, Integer precision, Integer scale) {
    addColumn(columnName, type, precision, scale, null, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable) {
    addColumn(columnName, type, precision, scale, nullable, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable, String comment) {
    if (type == null) {
      type = Types.VARCHAR;
    }
    SqlDataType sqlDataType = this.getDataPath().getConnection().getSqlDataType(type);
    if (sqlDataType == null) {
      throw new RuntimeException("The data store (" + this.getDataPath().getConnection() + ") does not know the numeric type code (" + type + ")");
    }
    this.addColumn(columnName, sqlDataType, precision, scale, nullable, comment);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, SqlDataType sqlDataType, Integer precision, Integer scale, Boolean nullable, String comment) {

    if (sqlDataType == null) {
      sqlDataType = this.getDataPath().getConnection().getSqlDataType(Types.VARCHAR);
    }
    int columnNullable;

    if (nullable == null) {
      columnNullable = DatabaseMetaData.columnNullableUnknown;
    } else if (nullable) {
      columnNullable = DatabaseMetaData.columnNullable;
    } else {
      columnNullable = DatabaseMetaData.columnNoNulls;
    }

    getOrCreateColumn(columnName, sqlDataType, sqlDataType.getSqlClass())
      .precision(precision)
      .scale(scale)
      .setNullable(columnNullable)
      .setComment(comment);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, Integer type, Integer precision, Boolean nullable) {
    addColumn(columnName, type, precision, null, nullable, null);
    return this;
  }


  /**
   * Return the columns by position
   * <p>
   * `? extends` to let the GenColumnDef return the type
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<? extends ColumnDef> getColumnDefs() {

    return new ArrayList<>(columnDefByName.values())
      .stream()
      .sorted(Comparator.comparing(ColumnDef::getColumnPosition))
      .collect(Collectors.toList());

  }


  /**
   * @param columnName the column name
   * @return the column or null if not found
   */
  public ColumnDef getColumnDef(String columnName) throws NoColumnException {

    ColumnDef column = columnDefByName.get(columnName);
    if (column == null) {
      throw new NoColumnException("The column (" + columnName + ") was not found for the table (" + this + ")");
    }
    return column;
  }

  /**
   * @return a columnDef by index starting at 0
   */
  public ColumnDef getColumnDef(Integer columnIndex) {

    return this.columnDefByName
      .values()
      .stream()
      .filter(c -> c.getColumnPosition().equals(columnIndex))
      .findFirst()
      .orElseThrow(()->new RuntimeException("No column at the index "+columnIndex));

  }


  /**
   * @param columnName - The column name
   * @param clazz      - The type of the column (Java needs the type to be a sort of type safe)
   * @return a new columnDef
   */
  public ColumnDef getOrCreateColumn(String columnName, SqlDataType sqlDataType, Class<?> clazz) {

    try {

      ColumnDef columnDefGet = getColumnDef(columnName);
      if (columnDefGet.getClazz() != clazz) {
        throw new IllegalStateException("The column (" + columnDefGet + ") was already defined (may be via metadata - database, datadef or datagen) with the type (" + columnDefGet.getDataType().getSqlName() + ") and then can not be changed to (" + sqlDataType.getSqlName() + ").");
      }
      return columnDefGet;

    } catch (NoColumnException e) {

      return createColumn(columnName, sqlDataType, clazz);

    }


  }

  /**
   * @param columnName - The column name
   * @param clazz      - The type of the column (Java needs the type to be a sort of type safe)
   * @return a new columnDef even if the column already existed
   */
  @Override
  public ColumnDef createColumn(String columnName, SqlDataType sqlDataType, Class<?> clazz) {


    // This assert is to catch when object are passed
    // to string function, the length is bigger than the assertion and make it fails
    assert columnName.length() < 100;
    ColumnDef columnDef = new ColumnDefBase(this, columnName, clazz, sqlDataType);
    ColumnDef oldColumn = columnDefByName.get(columnName);
    if (oldColumn == null) {
      columnDef.setColumnPosition(columnDefByName.size() + 1);
    } else {
      columnDef.setColumnPosition(oldColumn.getColumnPosition());
    }
    columnDefByName.put(columnName, columnDef);

    return columnDef;

  }

  @Override
  public int getColumnsSize() {
    return columnDefByName.size();
  }

  /**
   * @return an array of columns
   * The columns must exist otherwise you of a exception
   */
  protected ColumnDef[] getColumns(String... columnNames) throws NoColumnException {

    List<ColumnDef> columnDefs = new ArrayList<>();
    for (String columnName : columnNames) {
      final ColumnDef column;
      column = getColumnDef(columnName);
      columnDefs.add(column);

    }
    return columnDefs.toArray(new ColumnDef[0]);
  }

  /**
   * @return an array of columns
   * The columns must exist otherwise you of a exception
   */
  protected ColumnDef[] getColumns(List<String> columnNames) throws NoColumnException {

    return getColumns(columnNames.toArray(new String[0]));
  }

  @Override
  public String toString() {
    return "TableDef of " + getDataPath();
  }

  @Override
  public RelationDefDefault dropAll() {
    columnDefByName = new MapKeyIndependent<>();
    return this;
  }

  @Override
  public boolean hasColumn(String columnName) {
    return columnDefByName.containsKey(columnName);
  }


}
