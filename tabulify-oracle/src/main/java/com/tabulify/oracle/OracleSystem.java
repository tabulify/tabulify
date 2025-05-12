package com.tabulify.oracle;

import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.jdbc.SqlMetaColumn;
import com.tabulify.jdbc.SqlMetaDataType;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.UniqueKeyDef;
import com.tabulify.transfer.TransferSourceTarget;
import oracle.jdbc.OracleTypes;

import java.sql.Types;
import java.util.List;
import java.util.Map;

public class OracleSystem extends SqlDataSystem {


  public static final int MAX_PRECISION_NUMERIC = 38;
  public static final int MAXIMUM_SCALE_NUMERIC = 127;
  public static final int MINIMUM_SCALE_NUMERIC = -84;
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

  /**
   * Driver Data Type
   * <p>General Info</p>
   * <a href="https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT313">...</a>
   * To Java Class
   * <a href="https://docs.oracle.com/cd/E11882_01/java.112/e16548/apxref.htm#JJDBC28906">...</a>
   */
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

    // oracle.sql.NUMBER.class
    // We don't set the native class: oracle.sql.NUMBER.class
    // because the generator does not know it

    /**
     * Same as {@link OracleTypes.NUMBER}
     */
    sqlDataTypes.computeIfAbsent(Types.NUMERIC, SqlMetaDataType::new)
      .setSqlName("NUMBER")
      .setMaxPrecision(MAX_PRECISION_NUMERIC)
      .setMaximumScale(MAXIMUM_SCALE_NUMERIC)
      .setMinimumScale(MINIMUM_SCALE_NUMERIC);

    // https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA
    // We don't use the class oracle.sql.NUMBER.class
    // because the generator does not know it
    sqlDataTypes.computeIfAbsent(Types.DOUBLE, SqlMetaDataType::new)
      .setSqlName("NUMBER")
      .setMaxPrecision(MAX_PRECISION_NUMERIC)
      .setMaximumScale(MAXIMUM_SCALE_NUMERIC)
      .setMinimumScale(MINIMUM_SCALE_NUMERIC);

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
  private String getMergeStatement(TransferSourceTarget transferSourceTarget, boolean sqlBindFormat) {


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
    List<ColumnDef> uniqueKeyColumns = targetUniqueKeysFoundInSourceColumns.get(0).getColumns();
    for (int i = 0; i < uniqueKeyColumns.size(); i++) {
      ColumnDef uniqueKeyColumnDef = uniqueKeyColumns.get(i);
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
    List<ColumnDef> sourceNonUniqueColumnsForTarget = transferSourceTarget.getSourceNonUniqueColumnsForTarget();
    for (int i = 0; i < sourceNonUniqueColumnsForTarget.size(); i++) {
      ColumnDef updateColumn = sourceNonUniqueColumnsForTarget.get(i);
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
    List<? extends ColumnDef> targetColumnsToLoad = transferSourceTarget.getSourceColumnsInInsertStatement();
    for (int i = 0; i < targetColumnsToLoad.size(); i++) {
      ColumnDef targetInsertColumn = targetColumnsToLoad.get(i);
      mergeStatement.append(this.createQuotedName(targetInsertColumn.getColumnName()));
      if (i != targetColumnsToLoad.size() - 1) {
        mergeStatement.append(",");
      }
    }
    mergeStatement.append(") VALUES (");
    for (int i = 0; i < targetColumnsToLoad.size(); i++) {
      ColumnDef targetInsertColumn = targetColumnsToLoad.get(i);
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
  public String createUpsertStatementWithPrintfExpressions(TransferSourceTarget transferSourceTarget) {
    return getMergeStatement(transferSourceTarget, false);
  }


  @Override
  public String createUpsertStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {

    return getMergeStatement(transferSourceTarget, true);

  }

  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {
    List<SqlMetaColumn> sqlMetaColumns = super.getMetaColumns(dataPath);

    for (SqlMetaColumn metaColumn : sqlMetaColumns) {
      // bug, date is returned as timestamp (93) instead of date (91)
      if (metaColumn.getTypeName().equals("DATE")) {
        metaColumn.setTypeCode(Types.DATE);
      }
      // https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-7690645A-0EE3-46CA-90DE-C96DF5A01F8F
      // Specify an integer using the following form:
      // NUMBER(p)
      // ie scale is zero
      if (metaColumn.getTypeCode().equals(Types.NUMERIC) && metaColumn.getScale().equals(0)) {
        metaColumn.setTypeCode(Types.INTEGER);
      }
    }
    return sqlMetaColumns;
  }


}
