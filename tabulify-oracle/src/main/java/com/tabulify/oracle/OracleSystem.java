package com.tabulify.oracle;

import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.jdbc.SqlMetaDataType;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import oracle.jdbc.OracleTypes;

import java.sql.Types;
import java.util.List;
import java.util.Map;

public class OracleSystem extends SqlDataSystem {


  public static int MAX_VARCHAR2_PRECISION_BYTE = 4000;

  @Override
  protected List<String> createTruncateStatement(List<SqlDataPath> dataPaths) {
    // return "truncate from " + dataPath.toSqlStringPath();
    return super.createTruncateStatement(dataPaths);
  }

  @Override
  protected String createDataTypeStatement(ColumnDef columnDef) {

    SqlDataType dataType = columnDef.getDataType();
    switch (dataType.getTypeCode()) {
      case Types.INTEGER:
        return "INTEGER";
      case OracleTypes.INTERVALDS:
        return "INTERVAL DAY (" + columnDef.getPrecision() + ") TO SECOND (" + columnDef.getScale() + ")";
      case OracleTypes.INTERVALYM:
        return "INTERVAL YEAR (" + columnDef.getPrecision() + ") TO MONTH";
      case OracleTypes.LONGVARBINARY:
        return "LONG RAW";
      case Types.LONGNVARCHAR:
        return "LONG";
      case OracleTypes.NUMBER:
        // Bug ? If the scale is -127, it's a float
        Integer precision = columnDef.getPrecision();
        Integer scale = columnDef.getScale();
        if (scale == -127 && precision != 0) {
          return "FLOAT(" + precision + ")";
        }
        // Default will take over
        if (precision > 38) {
          precision = 38;
        }
        return "NUMBER(" + precision + "," + scale + ")";
      case Types.VARBINARY:
        // Bug in a Oracle driver where precision is null in a resultSet
        if (columnDef.getPrecision() == 0) {
          return "RAW(2000)";
        }
        return super.createDataTypeStatement(columnDef);
      default:
        return super.createDataTypeStatement(columnDef);
    }

  }

  /**
   * 4000 bytes
   * <a href="https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1825">...</a>
   * <p></p>
   * if AL16UTF16 -> 1 char = 2 bytes
   * if UTF8      -> 1 char = 3 bytes
   */
  public static final int MAX_NVARCHAR_PRECISION_BYTE = 4000;
  protected static final int MAX_NCHAR_PRECISION_BYTE = 2000;

  // https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#i1960
  // max precision is 2000 in char or bytes
  protected static final int MAX_CHAR_PRECISION_BYTE_OR_CHAR = 2000;

  public OracleSystem(OracleConnection oracleConnection) {
    super(oracleConnection);
  }

  // https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT313
  @Override
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {

    Map<Integer, SqlMetaDataType> sqlDataTypes = super.getMetaDataTypes();

    sqlDataTypes.computeIfAbsent(OracleTypes.INTERVALDS, SqlMetaDataType::new)
      .setSqlName("INTERVALDS")
      .setSqlJavaClazz(oracle.sql.INTERVALDS.class);

    sqlDataTypes.computeIfAbsent(OracleTypes.INTERVALYM, SqlMetaDataType::new)
      .setSqlName("INTERVAL_YEAR_MONTH")
      .setSqlJavaClazz(oracle.sql.INTERVALYM.class);

    sqlDataTypes.computeIfAbsent(OracleTypes.LONGVARBINARY, SqlMetaDataType::new)
      .setSqlName("LONG RAW")
      .setSqlJavaClazz(oracle.sql.RAW.class);

    sqlDataTypes.computeIfAbsent(Types.LONGVARCHAR, SqlMetaDataType::new)
      .setSqlName("LONG");

    // https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA
    sqlDataTypes.computeIfAbsent(OracleTypes.NUMBER, SqlMetaDataType::new)
      .setSqlName("NUMBER")
      .setSqlJavaClazz(oracle.sql.NUMBER.class)
      .setMaxPrecision(38)
      .setMaximumScale(127)
      .setMinimumScale(-84);

    // https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA
    sqlDataTypes.computeIfAbsent(Types.DOUBLE, SqlMetaDataType::new)
      .setSqlName("NUMBER")
      .setSqlJavaClazz(oracle.sql.NUMBER.class)
      .setMaxPrecision(38)
      .setMaximumScale(127)
      .setMinimumScale(-84);


    /**
     * Oracle Database supports a reliable Unicode datatype through NCHAR, NVARCHAR2, and NCLOB.
     * <p></p>
     * These datatypes are guaranteed to be Unicode encoding and always use character length semantics.
     * (ie the maximum size is always in character length semantics)
     */
    String nCharacterSet = this.getConnection().getMetadata().getUnicodeCharacterSet();
    int bytesByNChar = 3;
    switch (nCharacterSet) {
      case "AL16UTF16":
        bytesByNChar = 2; // The AL16UTF16 use 2 bytes to store a character.
        break;
      case "UTF8":
        //noinspection DataFlowIssue
        bytesByNChar = 3; // UTF 8 - 3
        break;
    }
    int maxNvarcharPrecision = MAX_NVARCHAR_PRECISION_BYTE / bytesByNChar;
    sqlDataTypes.computeIfAbsent(Types.NVARCHAR, SqlMetaDataType::new)
      .setSqlName("NVARCHAR2")
      .setMaxPrecision(maxNvarcharPrecision)
      .setPrecisionMandatory(true)
      .setDefaultPrecision(maxNvarcharPrecision);

    int maxNcharPrecision = MAX_NCHAR_PRECISION_BYTE / bytesByNChar;
    sqlDataTypes.computeIfAbsent(Types.NCHAR, SqlMetaDataType::new)
      .setSqlName("NCHAR")
      .setMaxPrecision(maxNcharPrecision)
      .setPrecisionMandatory(true)
      .setDefaultPrecision(maxNcharPrecision);

    String characterSet = this.getConnection().getMetadata().getCharacterSet();
    int bytesByChar = 3;
    switch (characterSet) {
      case "AL32UTF8":
        bytesByChar = 2; // The AL16UTF16 use 2 bytes to store a character.
        break;
    }


    sqlDataTypes.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setSqlName("CHAR")
      .setMaxPrecision(MAX_CHAR_PRECISION_BYTE_OR_CHAR)
      .setDefaultPrecision(1);

    int maxVarcharPrecision = MAX_VARCHAR2_PRECISION_BYTE / bytesByChar;
    sqlDataTypes.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setSqlName("VARCHAR2")
      .setMaxPrecision(maxVarcharPrecision)
      .setDefaultPrecision(maxVarcharPrecision)
      .setPrecisionMandatory(true);

    sqlDataTypes.computeIfAbsent(Types.VARBINARY, SqlMetaDataType::new)
      .setSqlName("VARBINARY")
      .setSqlJavaClazz(oracle.sql.RAW.class);

    /**
     * https://docs.oracle.com/javadb/10.10.1.2/ref/rrefclob.html
     */
    sqlDataTypes.computeIfAbsent(Types.CLOB, SqlMetaDataType::new)
      .setSqlName("CLOB")
      .setMaxPrecision(null); // no precision

    return sqlDataTypes;
  }


  @Override
  public OracleConnection getConnection() {
    return (OracleConnection) super.getConnection();
  }


}
