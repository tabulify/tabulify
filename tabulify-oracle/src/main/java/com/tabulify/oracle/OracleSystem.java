  package com.tabulify.oracle;

import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.jdbc.SqlMetaDataType;
import oracle.jdbc.OracleTypes;

import java.sql.Types;
import java.util.Map;

public class OracleSystem extends SqlDataSystem {


  public static int MAX_VARCHAR2_PRECISION_BYTE = 4000;

  /**
   * 4000 bytes
   * https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1825
   *
   * if AL16UTF16 -> 1 char = 2 bytes
   * if UTF8      -> 1 char = 3 bytes
   */
  public static final int MAX_NVARCHAR_PRECISION_BYTE = 4000;
  protected static final int MAX_NCHAR_PRECISION_BYTE = 2000;

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

    sqlDataTypes.computeIfAbsent( OracleTypes.LONGVARBINARY, SqlMetaDataType::new)
      .setSqlName("LONG RAW")
      .setSqlJavaClazz(oracle.sql.RAW.class);

    sqlDataTypes.computeIfAbsent(Types.LONGVARCHAR, SqlMetaDataType::new)
      .setSqlName("LONG");

    // https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA
    sqlDataTypes.computeIfAbsent( OracleTypes.NUMBER, SqlMetaDataType::new)
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
     *
     * These datatypes are guaranteed to be Unicode encoding and always use character length semantics.
     * (ie the maximum size is always in character length semantics)
     */
    String nCharacterSet = this.getConnection().getMetadata().getUnicodeCharacterSet();
    Integer bytesByNChar = 3;
    switch (nCharacterSet){
      case "AL16UTF16":
        bytesByNChar = 2; // The AL16UTF16 use 2 bytes to store a character.
        break;
      case "UTF8":
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
    Integer bytesByChar = 3;
    switch (characterSet){
      case "AL32UTF8":
        bytesByChar = 2; // The AL16UTF16 use 2 bytes to store a character.
        break;
    }
    sqlDataTypes.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setSqlName("CHAR")
      .setDefaultPrecision(1);

    int maxVarcharPrecision = MAX_VARCHAR2_PRECISION_BYTE / bytesByChar;
    sqlDataTypes.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setSqlName("VARCHAR2")
      .setMaxPrecision(maxVarcharPrecision)
      .setDefaultPrecision(maxVarcharPrecision)
      .setPrecisionMandatory(true);

    sqlDataTypes.computeIfAbsent( Types.VARBINARY, SqlMetaDataType::new)
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
