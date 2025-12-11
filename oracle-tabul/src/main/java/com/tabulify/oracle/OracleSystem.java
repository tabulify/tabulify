package com.tabulify.oracle;

import com.tabulify.jdbc.*;
import com.tabulify.model.*;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.transfer.TransferSourceTargetOrder;
import oracle.jdbc.OracleTypes;

import java.sql.Types;
import java.util.List;
import java.util.Set;

public class OracleSystem extends SqlDataSystem {


  public static final int MAX_PRECISION_NUMERIC = 38;
  public static final int MAXIMUM_SCALE_NUMERIC = 127;
  public static final int MINIMUM_SCALE_NUMERIC = -84;
  public static final int MAX_NUMERIC_PRECISION = 38;

  /**
   * Ref: You can omit size from the column definition. The default value is 1.
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html">...</a>
   */
  public static final int CHAR_DEFAULT_PRECISION = 1;
  // The AL32UTF8 character set implements the UTF-8 encoding form and supports the latest version of the Unicode standard.
  // It encodes characters in one, two, three, or four bytes. Supplementary characters require four bytes. It is for ASCII-based platforms.
  // https://docs.oracle.com/database/121/NLSPG/ch6unicode.htm
  // ```
  public static final int AL32UTF8_MAX_BYTES_BY_CHAR = 4;
  /**
   * The NCHAR default value is 1
   * Ref: <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#SQLRF-GUID-FE15E51B-52C6-45D7-9883-4DF477">...</a>16A17D
   */
  static final int NCHAR_DEFAULT_PRECISION = 1;
  public static final int AL16UTF16_MAX_BYTES_BY_CHAR = 2;
  /**
   * Max precision is unit independent (then in byte)
   */
  public static int VARCHAR2_MAX_PRECISION_BYTE = 4000;

  @Override
  protected List<String> createTruncateStatement(List<SqlDataPath> dataPaths) {
    // return "truncate from " + dataPath.toSqlStringPath();
    return super.createTruncateStatement(dataPaths);
  }

  @Override
  protected String createDataTypeStatement(ColumnDef<?> columnDef) {

    SqlDataType<?> dataType = columnDef.getDataType();

    /**
     * Date is a timestamp/datetime type but without precision
     * We take over to return the `date` without any precision at all (Driver returns 7)
     * https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-A3C0D836-BADB-44E5-A5D4-265BA5968483__GUID-0EA41E53-451F-4ECE-8523-5FC4C5A37977
     */
    if (dataType.toKeyNormalizer().equals(SqlDataTypeAnsi.DATE.toKeyNormalizer())) {
      return "date";
    }

    int precision = columnDef.getPrecision();
    int defaultPrecision = dataType.getDefaultPrecision();
    switch (dataType.getVendorTypeNumber()) {
      case Types.CHAR:
      case Types.VARCHAR:
        /**
         * Adding the char qualifier
         * (ie varchar2(3 char))
         * No unit length specifier for NCHAR and NVARCHAR
         * (ie nchar(3), not nchar(3 byte) or nchar(3 char)
         */
        if (precision == 0) {
          precision = defaultPrecision;
        }
        if (precision == defaultPrecision && !dataType.getIsSpecifierMandatory()) {
          return columnDef.getDataType().toKeyNormalizer().toSqlTypeCase();
        }
        return columnDef.getDataType().toKeyNormalizer().toSqlTypeCase() + "(" + columnDef.getPrecisionOrMax() + " char)";
      case Types.INTEGER:
        // Integer does not really exist
        // Specify an integer using the following form:
        // NUMBER(p)
        // https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-7690645A-0EE3-46CA-90DE-C96DF5A01F8F
        return "NUMBER(" + columnDef.getPrecisionOrMax() + ")";
      case OracleTypes.INTERVALDS:
        return "INTERVAL DAY (" + columnDef.getPrecision() + ") TO SECOND (" + columnDef.getScale() + ")";
      case OracleTypes.INTERVALYM:
        return "INTERVAL YEAR (" + columnDef.getPrecision() + ") TO MONTH";
      case OracleTypes.LONGVARBINARY:
        return "LONG RAW";
      case Types.LONGNVARCHAR:
        return "LONG";
      case Types.NUMERIC:
        //case OracleTypes.NUMBER:
        // Bug ? If the scale is -127, it's a float
        if (precision == 0) {
          return "NUMBER(" + MAX_NUMERIC_PRECISION + ")";
        }
        int scale = columnDef.getScale();
        if (scale == 0) {
          // should have been an integer
          return "NUMBER(" + precision + ")";
        }
        if (scale == -127) {
          return "FLOAT(" + precision + ")";
        }
        // Default will take over
        if (precision > MAX_NUMERIC_PRECISION) {
          precision = MAX_NUMERIC_PRECISION;
        }
        return "NUMBER(" + precision + "," + scale + ")";
      case Types.VARBINARY:
        // Bug in an Oracle driver where precision is null in a resultSet
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
  protected static final int NCHAR_MAX_PRECISION_BYTE = 2000;

  // https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#i1960
  // max precision is 2000 (not unit dependent ie in char or bytes)
  protected static final int MAX_CHAR_PRECISION_BYTE_OR_CHAR = 2000;

  public OracleSystem(OracleConnection oracleConnection) {
    super(oracleConnection);
  }


  @Override
  public Set<SqlDataTypeVendor> getSqlDataTypeVendors() {
    return Set.of(OracleSqlType.values());
  }

  /**
   * Driver Data Type
   * <p>General Info</p>
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html">SQL Reference</a>
   * <a href="https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT313">...</a>
   * To Java Class
   * <a href="https://docs.oracle.com/cd/E11882_01/java.112/e16548/apxref.htm#JJDBC28906">...</a>
   */
  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager typeManager) {

    super.dataTypeBuildingMain(typeManager);

    /**
     * Date/timestamp and Date/time are in the driver
     * but not date/date
     */
    typeManager.createTypeBuilder(OracleSqlType.DATE_DATE)
      .setAnsiType(OracleSqlType.DATE_DATE.getAnsiType());

    /**
     * Driver returns 11 for max precision, clearly error
     */
    typeManager.getTypeBuilder(OracleSqlType.TIMESTAMP)
      .setMaxPrecision(OracleSqlType.TIMESTAMP.getMaxPrecision());

    /**
     * Real and double are not Oracle Data Type (is not in the list return by the driver)
     * but they can be used because of the ANSI data type mapping
     * https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF
     */
    typeManager.createTypeBuilder(SqlDataTypeAnsi.REAL)
      .setDescription("ANSI data type converted to Oracle Float(63)");
    typeManager.createTypeBuilder(SqlDataTypeAnsi.DOUBLE_PRECISION)
      .setDescription("ANSI data type converted to Oracle Float(126)");


    /**
     * The max precision is unit independent given in byte, we transform it in character
     * <p>
     * Ref: The maximum value of size is 2000, which means 2000 bytes or characters (code points), depending on the selected length semantics.
     * https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-1BABC478-FB47-4962-9B0C-8B8BD059E733
     * <p>
     * Oracle Database supports a reliable Unicode datatype through NCHAR, NVARCHAR2, and NCLOB.
     * <p></p>
     * These datatypes are guaranteed to be Unicode encoding and always use character length semantics.
     * (ie the maximum size is always in character length semantics)
     */
    String nCharacterSet = this.getConnection().getMetadata().getUnicodeCharacterSet();
    int bytesByNChar;
    // Ref
    // https://docs.oracle.com/database/121/NLSPG/ch6unicode.htm#NLSPG-GUID-6B549B3B-90DE-478B-A183-553D1A018996
    switch (nCharacterSet) {
      case "AL16UTF16":
        // The AL16UTF16 use 2 bytes to store a character.
        // Ref:
        // When the national character set is AL16UTF16, the maximum number of characters never occupies more bytes than the maximum capacity, as each character (in an Oracle sense) occupies exactly 2 bytes.
        // https://docs.oracle.com/database/121/NLSPG/ch6unicode.htm#NLSPG-GUID-6B549B3B-90DE-478B-A183-553D1A018996
        bytesByNChar = AL16UTF16_MAX_BYTES_BY_CHAR;
        break;
      case "UTF8":
        // The UTF8 character set implements the CESU-8 encoding form and encodes characters in one, two, or three bytes.
        // Ref: https://docs.oracle.com/database/121/NLSPG/ch6unicode.htm
        bytesByNChar = 3;
        break;
      default:
        // Ref:
        // If you want national character set columns to be able to hold the declared number of characters in any national character set, do not declare NCHAR columns longer than 2000/3=666 characters and NVARCHAR2 columns longer than 4000/3=1333 or 32767/3=10922 characters, depending on the MAX_STRING_SIZE initialization parameter.
        // https://docs.oracle.com/database/121/NLSPG/ch6unicode.htm#NLSPG-GUID-6B549B3B-90DE-478B-A183-553D1A018996
        bytesByNChar = 3;
        break;
    }

    int maxNvarcharPrecision = MAX_NVARCHAR_PRECISION_BYTE / bytesByNChar;
    typeManager.getTypeBuilder(OracleSqlType.NVARCHAR2)
      .setMaxPrecision(maxNvarcharPrecision)
      .setDefaultPrecision(maxNvarcharPrecision);

    int maxNcharPrecision = NCHAR_MAX_PRECISION_BYTE / bytesByNChar;
    typeManager.getTypeBuilder(OracleSqlType.NCHAR)
      .setMaxPrecision(maxNcharPrecision)
      .setDefaultPrecision(NCHAR_DEFAULT_PRECISION);


    String characterSet = this.getConnection().getMetadata().getCharacterSet();
    int maxBytesByChar;
    switch (characterSet) {
      case "AL32UTF8":
        // With a max of 2000, we get 500
        // It's what oracle recommend
        // ```
        // If you want a CHAR column to be always able to store size characters in any database character set, use a value of size that is
        // less than or equal to 500.
        // Ref: https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-1BABC478-FB47-4962-9B0C-8B8BD059E733
        maxBytesByChar = AL32UTF8_MAX_BYTES_BY_CHAR;
        break;
      default:
        maxBytesByChar = 3;
        break;
    }

    typeManager.getTypeBuilder(OracleSqlType.CHAR)
      .setMaxPrecision(MAX_CHAR_PRECISION_BYTE_OR_CHAR / maxBytesByChar)
      .setDefaultPrecision(CHAR_DEFAULT_PRECISION);

    int maxVarcharPrecision = VARCHAR2_MAX_PRECISION_BYTE / maxBytesByChar;
    typeManager.getTypeBuilder(OracleSqlType.VARCHAR2)
      .setMaxPrecision(maxVarcharPrecision)
      .setDefaultPrecision(maxVarcharPrecision)
      .setPriority(SqlDataTypePriority.TOP)
      .setMandatorySpecifier(true);


  }


  @Override
  public OracleConnection getConnection() {
    return (OracleConnection) super.getConnection();
  }


  /**
   * Called a merge
   * <a href="https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_9016.htm#SQLRF01606">...</a>
   * MERGE INTO bonuses D
   * USING (SELECT employee_id, salary, department_id FROM employees
   * WHERE department_id = 80) S
   * ON (D.employee_id = S.employee_id)
   * WHEN MATCHED THEN UPDATE SET D.bonus = D.bonus + S.salary*.01
   * DELETE WHERE (S.salary > 8000)
   * WHEN NOT MATCHED THEN INSERT (D.employee_id, D.bonus)
   * VALUES (S.employee_id, S.salary*.01)
   * WHERE (S.salary <= 8000);
   */
  private String getMergeStatement(TransferSourceTargetOrder transferSourceTarget, boolean sqlBindFormat) {


    List<UniqueKeyDef> targetUniqueKeysFoundInSourceColumns = getTargetUniqueKeysFoundInSourceColumns(transferSourceTarget);
    if (targetUniqueKeysFoundInSourceColumns.isEmpty()) {
      if (sqlBindFormat) {
        return createInsertStatementWithBindVariables(transferSourceTarget);
      }
      return createInsertStatementWithPrintfExpressions(transferSourceTarget);
    }

    transferSourceTarget.checkBeforeInsert();

    RelationDef target = transferSourceTarget.getTargetDataPath().getOrCreateRelationDef();
    final SqlDataPath targetDataPath = (SqlDataPath) target.getDataPath();
    StringBuilder mergeStatement = new StringBuilder();
    String targetAlias = "target";
    mergeStatement.append("merge into ")
      .append(targetDataPath.toSqlStringPath())
      .append(" ") // as does not exist on oracle
      .append(targetAlias);
    String sourceAlias = "source";
    String values = createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, sqlBindFormat, true);
    mergeStatement.append(" using (select ")
      .append(values)
      .append(" from dual) ") // no `as` please, not supported by oracle
      .append(sourceAlias);

    // on
    mergeStatement.append(" on (");
    List<ColumnDef<?>> uniqueKeyColumns = targetUniqueKeysFoundInSourceColumns.get(0).getColumns();
    for (int i = 0; i < uniqueKeyColumns.size(); i++) {
      ColumnDef<?> uniqueKeyColumnDef = uniqueKeyColumns.get(i);
      mergeStatement
        .append(sourceAlias).append(".").append(this.createQuotedName(uniqueKeyColumnDef.getColumnName()))
        .append(" = ")
        .append(targetAlias).append(".").append(this.createQuotedName(uniqueKeyColumnDef.getColumnName()));
      if (i != uniqueKeyColumns.size() - 1) {
        mergeStatement.append(",");
      }
    }
    mergeStatement.append(")");

    // WHEN MATCHED THEN update
    mergeStatement.append(" WHEN MATCHED THEN UPDATE SET ");
    List<ColumnDef<?>> sourceNonUniqueColumnsForTarget = transferSourceTarget.getSourceNonUniqueColumnsForTarget();
    for (int i = 0; i < sourceNonUniqueColumnsForTarget.size(); i++) {
      ColumnDef<?> updateColumn = sourceNonUniqueColumnsForTarget.get(i);
      mergeStatement
        .append(targetAlias)
        .append(".")
        .append(this.createQuotedName(updateColumn.getColumnName()))
        .append(" = ")
        .append(sourceAlias)
        .append(".")
        .append(this.createQuotedName(updateColumn.getColumnName()));
      if (i != sourceNonUniqueColumnsForTarget.size() - 1) {
        mergeStatement.append(",");
      }
    }

    // WHEN not MATCHED THEN insert
    mergeStatement.append(" WHEN NOT MATCHED THEN INSERT (");
    List<? extends ColumnDef<?>> targetColumnsToLoad = transferSourceTarget.getTargetColumnInInsertStatement();
    for (int i = 0; i < targetColumnsToLoad.size(); i++) {
      ColumnDef<?> targetInsertColumn = targetColumnsToLoad.get(i);
      mergeStatement.append(this.createQuotedName(targetInsertColumn.getColumnName()));
      if (i != targetColumnsToLoad.size() - 1) {
        mergeStatement.append(",");
      }
    }
    mergeStatement.append(") VALUES (");
    for (int i = 0; i < targetColumnsToLoad.size(); i++) {
      ColumnDef<?> targetInsertColumn = targetColumnsToLoad.get(i);
      mergeStatement
        .append(sourceAlias)
        .append(".")
        .append(this.createQuotedName(targetInsertColumn.getColumnName()));
      if (i != targetColumnsToLoad.size() - 1) {
        mergeStatement.append(",");
      }
    }
    mergeStatement.append(")");

    return mergeStatement.toString();
  }

  @Override
  public String createUpsertMergeStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return getMergeStatement(transferSourceTarget, false);
  }


  @Override
  public String createUpsertMergeStatementWithParameters(TransferSourceTargetOrder transferSourceTarget) {

    return getMergeStatement(transferSourceTarget, true);

  }


  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {
    List<SqlMetaColumn> metaColumns = super.getMetaColumns(dataPath);
    for (SqlMetaColumn sqlMetaColumn : metaColumns) {
      // Timestamp correction
      // Unfortunately, the type in the table column is with precision (ie "TIMESTAMP(3)" and not "TIMESTAMP")
      sqlMetaColumn.setTypeName(OracleSqlUtil.normalizeTimestampType(sqlMetaColumn.getTypeName()));
      // The driver sends us the type numeric/type code integer
      // but does not make the conversion when reading columns
      // we do it here
      if (sqlMetaColumn.getTypeCode() == Types.NUMERIC) {
        if (sqlMetaColumn.getDecimalDigits() == 0) {
          if (sqlMetaColumn.getColumnSize() <= 1) {
            sqlMetaColumn.setTypeCode(Types.BIT);
          } else if (sqlMetaColumn.getColumnSize() <= 3) {
            sqlMetaColumn.setTypeCode(Types.TINYINT);
          } else if (sqlMetaColumn.getColumnSize() <= 5) {
            sqlMetaColumn.setTypeCode(Types.SMALLINT);
          } else if (sqlMetaColumn.getColumnSize() <= 10) {
            sqlMetaColumn.setTypeCode(Types.INTEGER);
          } else {
            sqlMetaColumn.setTypeCode(Types.BIGINT);
          }
        }
      }
      if (sqlMetaColumn.getTypeCode() == Types.TIMESTAMP && sqlMetaColumn.getTypeName().equals("DATE")) {
        sqlMetaColumn.setDecimalDigits(0);
      }
    }
    return metaColumns;
  }

  @Override
  protected List<String> createDropStatement(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {
    SqlMediaType enumObjectType = sqlDataPaths.get(0).getMediaType();
    return SqlDropStatement.builder()
      .setType(enumObjectType)
      .setIsCascadeSupported(true)
      .setCascadeWord("cascade constraints")
      .setIfExistsSupported(true)
      .setMultipleSqlObjectSupported(false)
      .build()
      .getStatements(sqlDataPaths, dropAttributes);
  }
}
