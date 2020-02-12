package net.bytle.db.database.JdbcDataType;

import net.bytle.db.Tabular;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sql data type static function
 */
public class SqlDataTypes {

  private static final Set<SqlDataType> sqlDataTypes = new HashSet<>();

  private static final Map<Class, SqlDataType> sqlDataTypeByJavaClass = new HashMap<Class, SqlDataType>();

  static {
    sqlDataTypes.add(new ArraySql());
    sqlDataTypes.add(new BigIntSql());
    sqlDataTypes.add(new BinarySql());
    sqlDataTypes.add(new BitSql());
    sqlDataTypes.add(new BlobSql());
    sqlDataTypes.add(new BooleanSql());
    sqlDataTypes.add(new CharSql());
    sqlDataTypes.add(new ClobSql());
    sqlDataTypes.add(new DataLinkSql());
    sqlDataTypes.add(new DateSql());
    sqlDataTypes.add(new DecimalSql());
    sqlDataTypes.add(new DistinctSql());
    sqlDataTypes.add(new DoubleSql());
    sqlDataTypes.add(new FloatSql());
    sqlDataTypes.add(new IntegerSql());
    sqlDataTypes.add(new JavaObjectSql());
    sqlDataTypes.add(new LongNVarcharSql());
    sqlDataTypes.add(new LongVarBinarySql());
    sqlDataTypes.add(new LongVarcharSql());
    sqlDataTypes.add(new NCharSql());
    sqlDataTypes.add(new NClobSql());
    sqlDataTypes.add(new NullSql());
    sqlDataTypes.add(new NumericSql());
    sqlDataTypes.add(new NVarcharSql());
    sqlDataTypes.add(new OtherSql());
    sqlDataTypes.add(new RealSql());
    sqlDataTypes.add(new RefSql());
    sqlDataTypes.add(new RowidSql());
    sqlDataTypes.add(new SmallIntSql());
    sqlDataTypes.add(new SqlXmlSql());
    sqlDataTypes.add(new StructSql());
    sqlDataTypes.add(new TimeSql());
    sqlDataTypes.add(new TimestampSql());
    sqlDataTypes.add(new TinyIntSql());
    sqlDataTypes.add(new VarBinarySql());
    sqlDataTypes.add(new VarcharSql());

    // Mapping from Java Type to Sql Type
    // From https://docs.oracle.com/javase/6/docs/technotes/guides/jdbc/getstart/mapping.html

    // From Pure Jdbc Type to Sql Type
    sqlDataTypeByJavaClass.put(long.class, new BigIntSql());
    sqlDataTypeByJavaClass.put(Boolean.class, new BitSql()); // No Boolean
    sqlDataTypeByJavaClass.put(URL.class, new DataLinkSql());
    sqlDataTypeByJavaClass.put(BigDecimal.class, new NumericSql()); // Of Decimal ?
    sqlDataTypeByJavaClass.put(Double.class, new DoubleSql()); // And no Float
    sqlDataTypeByJavaClass.put(Integer.class, new IntegerSql()); // And not small int, tinyInt
    sqlDataTypeByJavaClass.put(Object.class, new JavaObjectSql());
    sqlDataTypeByJavaClass.put(byte[].class, new LongVarBinarySql()); // No Binary of varbinary
    sqlDataTypeByJavaClass.put(byte.class, new TinyIntSql());
    sqlDataTypeByJavaClass.put(String.class, new VarcharSql()); // No char of varchar, LongNVarchar, nchar, nvarchar
    sqlDataTypeByJavaClass.put(Float.class, new RealSql());
    sqlDataTypeByJavaClass.put(Short.class, new SmallIntSql());
    sqlDataTypeByJavaClass.put(Long.class, new BigIntSql());


  }

  static public SqlDataType get(Integer typeCode) {
    List<SqlDataType> sqlDataTypes = SqlDataTypes.sqlDataTypes.stream().filter(s -> s.getTypeCode() == typeCode)
      .collect(Collectors.toList());
    switch (sqlDataTypes.size()) {
      case 0:
        return null;
      case 1:
        return sqlDataTypes.get(0);
      default:
        throw new RuntimeException("Too much sql data type found for the SQL type code (" + typeCode + "). Sql Types found " + sqlDataTypes);
    }
  }

  static public SqlDataType get(String typeName) {
    List<SqlDataType> sqlDataTypes = SqlDataTypes.sqlDataTypes.stream().filter(s -> s.getTypeName().toLowerCase().equals(typeName.toLowerCase()))
      .collect(Collectors.toList());
    switch (sqlDataTypes.size()) {
      case 0:
        return null;
      case 1:
        return sqlDataTypes.get(0);
      default:
        throw new RuntimeException("Too much sql data type found for the SQL type code (" + typeName + "). Sql Types found " + sqlDataTypes);
    }
  }

  public static SqlDataType ofClass(Class clazz) {
    final SqlDataType sqlDataType = sqlDataTypeByJavaClass.get(clazz);
    if (sqlDataType == null) {
      throw new RuntimeException("The jdbc data type for the class (" + clazz + ") is unknown");
    }
    return sqlDataType;
  }


  /**
   * Print SQL data type given by the database wrapper
   */
  public static void printSqlDataType() {

    DataPath dataPath = Tabular.tabular().getDataPath("datatype")
      .getDataDef()
      .addColumn("Data Type")
      .addColumn("Type Name")
      .getDataPath();

    try (InsertStream insertStream = Tabulars.getInsertStream(dataPath)) {
      sqlDataTypes.forEach(typeInfo -> insertStream.insert(typeInfo.getTypeCode(), typeInfo.getTypeName()));
    }
    Tabulars.print(dataPath);

  }

}
