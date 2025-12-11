package com.tabulify.model;


import com.tabulify.connection.Connection;
import com.tabulify.spi.DataPath;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoColumnException;
import com.tabulify.type.KeyInterface;
import com.tabulify.type.KeyNormalizer;

import java.util.*;
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


  public static final SqlDataTypeAnsi DEFAULT_DATA_TYPE = SqlDataTypeAnsi.CHARACTER_VARYING;

  /**
   * But Oracle by default put all name in uppercase when quoting is disabled
   * we should apply the same transform but RelationDef is system independent
   * so we make it case independent here
   */
  protected Map<KeyNormalizer, ColumnDef<?>> columnDefByName = new HashMap<>();

  public <T extends DataPath> RelationDefDefault(T DataPath) {
    super(DataPath);
  }


  public RelationDefDefault addColumn(String columnName) {
    addColumn(columnName, DEFAULT_DATA_TYPE, 0, 0, null, null);
    return this;
  }


  @Override
  public RelationDef addColumn(String columnName, SqlDataTypeAnsi ansiType) {
    /**
     * We search by type number because for
     * * one ANSI type, one code
     * * the ANSI name/type code is not really a SQL type identifier
     */
    SqlDataType<?> sqlDataType = this.getDataPath().getConnection().getSqlDataType(ansiType);
    if (sqlDataType == null) {
      throw getNotSupportedType(columnName, ansiType.toString());
    }
    return addColumn(columnName, sqlDataType);
  }

  public RelationDefDefault addColumn(String columnName, Class<?> clazz) {
    SqlDataType<?> typeCode = this.getDataPath().getConnection().getSqlDataType(clazz);
    addColumn(columnName, typeCode, 0, 0, null, null);
    return this;
  }


  public RelationDefDefault addColumn(String columnName, SqlDataTypeAnsi type, Boolean nullable) {
    addColumn(columnName, type, 0, 0, nullable, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, SqlDataTypeAnsi type, int precision, int scale) {
    addColumn(columnName, type, precision, scale, null, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, SqlDataTypeAnsi type, int precision, int scale, Boolean nullable) {
    addColumn(columnName, type, precision, scale, nullable, null);
    return this;
  }

  public RelationDefDefault addColumn(String columnName, SqlDataTypeAnsi type, int precision, int scale, Boolean nullable, String comment) {
    SqlDataType<?> sqlDataType = this.getDataPath().getConnection().getSqlDataType(type);
    if (sqlDataType == null) {

      if (type == SqlDataTypeAnsi.OTHER) {
        throw new IllegalArgumentException("You can't ask for the type code (" + SqlDataTypeAnsi.OTHER + ") for the column (" + columnName + ") on the resource (" + this.getDataPath() + ") because it's not a deterministic. Multiple type may have this type code.");
      }
      throw getNotSupportedType(columnName, type.toString());
    }
    return addColumn(columnName, sqlDataType, precision, scale, nullable, comment);
  }

  private IllegalArgumentException getNotSupportedType(String columnName, String type) {
    return new IllegalArgumentException("The connection (" + this.getDataPath().getConnection() + ") does not support the type (" + type + ") set on the column (" + columnName + ") of the resource (" + this.getDataPath() + ")");
  }


  public RelationDefDefault addColumn(String columnName, SqlDataType<?> sqlDataType, int precision, int scale, Boolean nullable, String comment) {
    Objects.requireNonNull(columnName, "column name should not be null");
    Objects.requireNonNull(sqlDataType, "sqlDataType should not be null for column " + columnName);

    createColumn(columnName, sqlDataType)
      .setPrecision(precision)
      .setScale(scale)
      .setNullable(SqlDataTypeNullable.cast(nullable))
      .setComment(comment);
    return this;
  }

  @Override
  public RelationDefDefault addColumn(String columnName, SqlDataTypeAnsi type, int precision, Boolean nullable) {
    addColumn(columnName, type, precision, 0, nullable, null);
    return this;
  }


  /**
   * Return the columns by position
   * <p>
   * `? extends` to let the GenColumnDef return the type
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<? extends ColumnDef<?>> getColumnDefs() {

    return new ArrayList<>(columnDefByName.values())
      .stream()
      .sorted(Comparator.comparing(ColumnDef::getColumnPosition))
      .collect(Collectors.toList());

  }


  /**
   * @param columnName the column name
   * @return the column or null if not found
   */
  public ColumnDef<?> getColumnDef(String columnName) throws NoColumnException {

    KeyNormalizer columnNameNormalized;
    try {
      columnNameNormalized = KeyNormalizer.create(columnName);
    } catch (CastException e) {
      throw new InternalException("The column name (" + columnName + ") is not valid. Error: " + e.getMessage(), e);
    }
    ColumnDef<?> column = columnDefByName.get(columnNameNormalized);
    if (column == null) {
      throw new NoColumnException("The column (" + columnName + ") was not found on the resource (" + this.getDataPath() + ")");
    }
    return column;
  }

  /**
   * @return a columnDef by index starting at 0
   */
  public ColumnDef<?> getColumnDef(Integer columnIndex) {

    if (columnIndex <= 0) {
      throw new IllegalArgumentException("The column index must be greater than 0. It starts at 1");
    }

    return this.columnDefByName
      .values()
      .stream()
      .filter(c -> c.getColumnPosition() == columnIndex)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("No column at the index " + columnIndex));

  }


  /**
   * @param columnName - The column name
   * @return a new columnDef
   */
  public <T> ColumnDef<T> getOrCreateColumn(String columnName, SqlDataType<T> sqlDataType) {

    try {

      ColumnDef<?> columnDefGet = getColumnDef(columnName);
      Class<T> valueClass = sqlDataType.getValueClass();
      if (columnDefGet.getClazz() != valueClass) {
        throw new IllegalStateException("The column (" + columnDefGet + ") was already defined (may be via metadata - database, manifest ) with the class (" + columnDefGet.getClazz().getName() + ") and then can not be changed to (" + valueClass.getName() + ").");
      }
      //noinspection unchecked
      return (ColumnDef<T>) columnDefGet;

    } catch (NoColumnException e) {

      return createColumn(columnName, sqlDataType);

    }


  }

  /**
   * @param columnName - The column name
   * @return a new columnDef even if the column already existed
   */
  @Override
  public <T> ColumnDef<T> createColumn(String columnName, SqlDataType<T> sqlDataType) {

    KeyNormalizer columnNormalized;
    try {
      columnNormalized = KeyNormalizer.create(columnName);
    } catch (CastException e) {
      throw new IllegalArgumentException("The column name (" + columnName + ") is not a valid name. Error: (" + e.getMessage() + ")", e);
    }
    // This assert is to catch when object are passed
    // to string function, the length is bigger than the assertion and make it fails
    assert columnName.length() < 100;
    ColumnDef<T> columnDef = new ColumnDefBase<>(this, columnName, sqlDataType);
    ColumnDef<?> oldColumn = columnDefByName.get(columnNormalized);
    // TODO: delete that, create should not replace,
    //  otherwise you would get some surprise
    if (oldColumn == null) {
      columnDef.setColumnPosition(columnDefByName.size() + 1);
    } else {
      columnDef.setColumnPosition(oldColumn.getColumnPosition());
    }
    columnDefByName.put(columnNormalized, columnDef);

    return columnDef;

  }

  @Override
  public int getColumnsSize() {
    return columnDefByName.size();
  }

  /**
   * @return an array of columns
   * The columns must exist otherwise you of an exception
   */
  protected ColumnDef<?>[] getColumns(String... columnNames) throws NoColumnException {

    List<ColumnDef<?>> columnDefs = new ArrayList<>();
    for (String columnName : columnNames) {
      final ColumnDef<?> column;
      column = getColumnDef(columnName);
      columnDefs.add(column);

    }
    return columnDefs.toArray(new ColumnDef[0]);
  }

  /**
   * @return an array of columns
   * The columns must exist otherwise you of a exception
   */
  protected ColumnDef<?>[] getColumns(List<String> columnNames) throws NoColumnException {

    return getColumns(columnNames.toArray(new String[0]));
  }

  @Override
  public String toString() {
    return "TableDef of " + getDataPath();
  }

  @Override
  public RelationDefDefault dropAll() {
    columnDefByName = new HashMap<>();
    return this;
  }

  @Override
  public boolean hasColumn(String columnName) {

    return hasColumn(KeyNormalizer.createSafe(columnName));
  }

  @Override
  public boolean hasColumn(KeyNormalizer columnName) {
    return columnDefByName.containsKey(columnName);
  }

  @Override
  public ColumnDef<?> getColumnDef(KeyNormalizer name) {
    return this.columnDefByName.get(name);
  }

  @Override
  public RelationDef addColumn(String columnName, SqlDataType<?> dataType) {
    return addColumn(columnName, dataType, 0, (short) 0, null, null);
  }

  @Override
  public RelationDef addColumn(String columnName, SqlDataTypeAnsi sqlDataTypeAnsi, int precision) {
    Connection connection = this.getDataPath().getConnection();
    SqlDataType<?> sqlDataType = connection.getSqlDataType(sqlDataTypeAnsi);
    if (sqlDataType == null) {
      throw getNotSupportedType(columnName, sqlDataTypeAnsi.toString());
    }
    return addColumn(columnName, sqlDataType, precision, (short) 0, null, null);
  }

  @Override
  public RelationDef addColumn(String columnName, KeyNormalizer typeName) {
    Connection connection = this.getDataPath().getConnection();
    SqlDataType<?> type = connection.getSqlDataType(typeName);
    if (type == null) {
      throw getNotSupportedType(columnName, typeName.toSqlTypeCase());
    }
    return addColumn(columnName, type);
  }

  @Override
  public <T> ColumnDef<T> getColumnDef(String columnName, Class<T> clazz) {
    ColumnDef<?> column = columnDefByName.get(KeyNormalizer.createSafe(columnName));
    if (column.getClazz() != clazz) {
      throw new InternalException("The column (" + columnName + ") has not the class (" + clazz.getName() + ") but the class (" + column.getClazz().getName() + ").");
    }
    //noinspection unchecked
    return (ColumnDef<T>) column;
  }

  @Override
  public RelationDef addColumn(String columnName, SqlDataTypeKeyInterface typeKeyInterface) {
    if (typeKeyInterface.getVendorTypeNumber() == 0) {
      return addColumn(columnName);
    }
    Connection connection = getDataPath().getConnection();
    SqlDataType<?> type = connection.getSqlDataType(typeKeyInterface);
    if (type == null) {
      throw getNotSupportedType(columnName, typeKeyInterface + "/" + typeKeyInterface.getVendorTypeNumber());
    }
    return addColumn(columnName, type);
  }

  @Override
  public RelationDef addColumn(String columnName, KeyInterface typeName) {
    return addColumn(columnName, typeName.toKeyNormalizer());
  }


}
