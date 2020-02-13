package net.bytle.db.database;

import net.bytle.db.Tabular;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Ref;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An object that groups sql data type operations
 */
public class SqlDataTypesManager {

  final static List<Integer> NUMERICS = Arrays.asList(Types.INTEGER, Types.DECIMAL, Types.NUMERIC, Types.DOUBLE);

  public static boolean isNumeric(int typeCode) {
    return NUMERICS.contains(typeCode);
  }

  private final Set<SqlDataType> sqlDataTypes;

  private final Map<Class, Integer> sqlDataTypeByJavaClass;


  public SqlDataTypesManager() {
    sqlDataTypes = new HashSet<>();
    sqlDataTypes.add(SqlDataType.of(Types.ARRAY).setTypeName("ARRAY").setClazz(java.sql.Array.class));
    sqlDataTypes.add(SqlDataType.of(Types.BIGINT).setTypeName("BIGINT").setClazz(long.class));
    sqlDataTypes.add(SqlDataType.of(Types.BINARY).setTypeName("BINARY").setClazz(byte[].class));
    sqlDataTypes.add(SqlDataType.of(Types.BIT).setTypeName("BIT").setClazz(Boolean.class));
    sqlDataTypes.add(SqlDataType.of(Types.BLOB).setTypeName("BLOB").setClazz(java.sql.Blob.class));
    sqlDataTypes.add(SqlDataType.of(Types.BOOLEAN).setTypeName("BOOLEAN").setClazz(Boolean.class));
    sqlDataTypes.add(SqlDataType.of(Types.CHAR).setTypeName("CHAR").setClazz(String.class));
    sqlDataTypes.add(SqlDataType.of(Types.CLOB).setTypeName("CLOB").setClazz(java.sql.Clob.class));
    sqlDataTypes.add(SqlDataType.of(Types.DATALINK).setTypeName("DATALINK").setClazz(java.net.URL.class));
    sqlDataTypes.add(SqlDataType.of(Types.DATE).setTypeName("DATE").setClazz(java.sql.Date.class));
    sqlDataTypes.add(SqlDataType.of(Types.DECIMAL).setTypeName("DECIMAL").setClazz(java.math.BigDecimal.class));
    sqlDataTypes.add(SqlDataType.of(Types.DISTINCT).setTypeName("DISTINCT"));
    sqlDataTypes.add(SqlDataType.of(Types.DOUBLE).setTypeName("DOUBLE").setClazz(Double.class));
    sqlDataTypes.add(SqlDataType.of(Types.FLOAT).setTypeName("FLOAT").setClazz(Double.class));
    sqlDataTypes.add(SqlDataType.of(Types.INTEGER).setTypeName("INTEGER").setClazz(Integer.class));
    sqlDataTypes.add(SqlDataType.of(Types.JAVA_OBJECT).setTypeName("JAVA_OBJECT").setClazz(Object.class));
    sqlDataTypes.add(SqlDataType.of(Types.LONGNVARCHAR).setTypeName("LONGNVARCHAR").setClazz(String.class));
    sqlDataTypes.add(SqlDataType.of(Types.LONGVARBINARY).setTypeName("LONGVARBINARY").setClazz(byte[].class));
    sqlDataTypes.add(SqlDataType.of(Types.LONGVARCHAR).setTypeName("LONGVARCHAR").setClazz(String.class));
    sqlDataTypes.add(SqlDataType.of(Types.NCHAR).setTypeName("NCHAR").setClazz(String.class).setDescription("setNString depending on the argument's size relative to the driver's limits on NVARCHAR"));
    sqlDataTypes.add(SqlDataType.of(Types.NCLOB).setTypeName("NCLOB").setClazz(java.sql.NClob.class));
    sqlDataTypes.add(SqlDataType.of(Types.NULL).setTypeName("NULL"));
    sqlDataTypes.add(SqlDataType.of(Types.NUMERIC).setTypeName("NUMERIC").setClazz(java.math.BigDecimal.class));
    sqlDataTypes.add(SqlDataType.of(Types.NVARCHAR).setTypeName("NVARCHAR").setClazz(String.class));
    sqlDataTypes.add(SqlDataType.of(Types.OTHER).setTypeName("OTHER"));
    sqlDataTypes.add(SqlDataType.of(Types.REAL).setTypeName("REAL").setClazz(Float.class));
    sqlDataTypes.add(SqlDataType.of(Types.REF).setTypeName("REF").setClazz(Ref.class));
    sqlDataTypes.add(SqlDataType.of(Types.ROWID).setTypeName("ROWID").setClazz(java.sql.RowId.class));
    sqlDataTypes.add(SqlDataType.of(Types.SMALLINT).setTypeName("SMALLINT").setClazz(Integer.class));
    sqlDataTypes.add(SqlDataType.of(Types.SQLXML).setTypeName("SQLXML").setClazz(java.sql.SQLXML.class));
    sqlDataTypes.add(SqlDataType.of(Types.STRUCT).setTypeName("STRUCT").setClazz(java.sql.Struct.class));
    sqlDataTypes.add(SqlDataType.of(Types.TIME).setTypeName("TIME").setClazz(java.sql.Time.class));
    sqlDataTypes.add(SqlDataType.of(Types.TIMESTAMP).setTypeName("TIMESTAMP").setClazz(java.sql.Timestamp.class));
    sqlDataTypes.add(SqlDataType.of(Types.TINYINT).setTypeName("TINYINT").setClazz(Integer.class));
    sqlDataTypes.add(SqlDataType.of(Types.VARBINARY).setTypeName("VARBINARY").setClazz(byte[].class));
    sqlDataTypes.add(SqlDataType.of(Types.VARCHAR).setTypeName("VARCHAR").setClazz(String.class));

    // See page `Mapping from Java Type to Sql Type` (https://docs.oracle.com/javase/6/docs/technotes/guides/jdbc/getstart/mapping.html)
    sqlDataTypeByJavaClass = new HashMap<>();
    sqlDataTypeByJavaClass.put(long.class, Types.BIGINT);
    sqlDataTypeByJavaClass.put(Long.class, Types.BIGINT);
    sqlDataTypeByJavaClass.put(Boolean.class, Types.BIT); // No Boolean
    sqlDataTypeByJavaClass.put(URL.class, Types.DATALINK);
    sqlDataTypeByJavaClass.put(BigDecimal.class, Types.NUMERIC); // Of Decimal ?
    sqlDataTypeByJavaClass.put(Double.class, Types.DOUBLE); // And no Float
    sqlDataTypeByJavaClass.put(Integer.class, Types.INTEGER); // And not small int, tinyInt
    sqlDataTypeByJavaClass.put(Object.class, Types.JAVA_OBJECT);
    sqlDataTypeByJavaClass.put(byte[].class, Types.LONGVARBINARY); // No Binary of varbinary
    sqlDataTypeByJavaClass.put(byte.class, Types.TINYINT);
    sqlDataTypeByJavaClass.put(String.class, Types.VARCHAR); // No char of varchar, LongNVarchar, nchar, nvarchar
    sqlDataTypeByJavaClass.put(Float.class, Types.REAL);
    sqlDataTypeByJavaClass.put(Short.class, Types.SMALLINT);

  }

  public SqlDataType get(Integer typeCode) {
    List<SqlDataType> foundSqlDataType = sqlDataTypes.stream().filter(s -> s.getTypeCode() == typeCode)
      .collect(Collectors.toList());
    switch (foundSqlDataType.size()) {
      case 0:
        return null;
      case 1:
        return foundSqlDataType.get(0);
      default:
        throw new RuntimeException("Too much sql data type found for the SQL type code (" + typeCode + "). Sql Types found " + foundSqlDataType);
    }
  }

  public SqlDataType get(String typeName) {
    List<SqlDataType> foundSqlDataType =
      sqlDataTypes
        .stream()
        .filter(s -> s.getTypeNames().stream().anyMatch(ty -> ty.toLowerCase().equals(typeName.toLowerCase())))
        .collect(Collectors.toList());
    switch (foundSqlDataType.size()) {
      case 0:
        return null;
      case 1:
        return foundSqlDataType.get(0);
      default:
        throw new RuntimeException("Too much sql data type found for the SQL type code (" + typeName + "). Sql Types found " + foundSqlDataType);
    }
  }

  public SqlDataType ofClass(Class clazz) {
    final Integer typeCode = sqlDataTypeByJavaClass.get(clazz);
    SqlDataType sqlDataType = null;
    if (typeCode != null) {
      sqlDataType = sqlDataTypes
        .stream()
        .filter(dt -> dt.getTypeCode() == typeCode)
        .findFirst()
        .orElse(null);
    }
    if (sqlDataType == null) {
      // trying to find it in the data type
      sqlDataType = sqlDataTypes
        .stream()
        .filter(dt->dt.getClazz()!=null)
        .filter(dt -> dt.getClazz().equals(clazz))
        .findFirst()
        .orElse(null);
    }
    if (sqlDataType == null){
      throw new RuntimeException("The jdbc data type for the class ("+clazz+") is unknown");
    } else {
      return sqlDataType;
    }

  }


  /**
   * Print SQL data type given by the database wrapper
   */
  public void printSqlDataType() {

    DataPath dataPath = Tabular.tabular().getDataPath("datatype")
      .getDataDef()
      .addColumn("Data Type")
      .addColumn("Type Name")
      .getDataPath();

    try (InsertStream insertStream = Tabulars.getInsertStream(dataPath)) {
      sqlDataTypes.forEach(typeInfo -> insertStream.insert(typeInfo.getTypeCode(), typeInfo.getTypeNames()));
    }
    Tabulars.print(dataPath);

  }

  public Set<SqlDataType> getDataTypes() {
    return sqlDataTypes;
  }

  public void addSqlDataType(SqlDataType sqlDataType) {
    sqlDataTypes.add(sqlDataType);
  }
}
