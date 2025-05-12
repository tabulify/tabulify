package com.tabulify.sqlserver;

import com.tabulify.jdbc.*;
import com.tabulify.model.*;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferSourceTarget;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SqlServerDataSystem extends SqlDataSystem {

  public static final int MAX_NVARCHAR_PRECISION = 4000;
  public static final int MAX_VARCHAR_PRECISION = 8000;

  public SqlServerDataSystem(SqlServerConnection jdbcDataStore) {
    super(jdbcDataStore);
  }

  /**
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/statements/truncate-table-transact-sql?view=sql-server-ver15">...</a>
   */
  @Override
  public void truncate(List<DataPath> dataPaths) {
    super.truncate(dataPaths);
  }

  @Override
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {
    Map<Integer, SqlMetaDataType> metaDataType = super.getMetaDataTypes();

    /**
     * Character
     */
    // The size (xxxxxxx) given to the column 'varchar' exceeds the maximum allowed for any data type (8000).
    metaDataType.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setDefaultPrecision(1)
      .setMaxPrecision(MAX_VARCHAR_PRECISION);

    metaDataType.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setDefaultPrecision(1);

    metaDataType.computeIfAbsent(Types.NCHAR, SqlMetaDataType::new)
      .setDefaultPrecision(1);

    // nvarchar is used to store json in the doc

    metaDataType.computeIfAbsent(Types.NVARCHAR, SqlMetaDataType::new)
      .setSqlName("nvarchar") // was sysname
      .setDefaultPrecision(1)
      .setMaxPrecision(MAX_NVARCHAR_PRECISION);

    /**
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     * Clob can be seen as text that comes back as long varchar
     */
    metaDataType.computeIfAbsent(Types.CLOB, SqlMetaDataType::new)
      .setSqlName("text");
    metaDataType.computeIfAbsent(Types.LONGVARCHAR, SqlMetaDataType::new)
      .setSqlName("text");

    /**
     * Integer
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql
     * The driver was adding the identity word
     * https://docs.microsoft.com/en-us/sql/t-sql/statements/create-table-transact-sql-identity-property
     */
    metaDataType.computeIfAbsent(Types.INTEGER, SqlMetaDataType::new)
      .setSqlName("int")
      .setMaxPrecision(10);

    metaDataType.computeIfAbsent(Types.BIGINT, SqlMetaDataType::new)
      .setSqlName("bigint")
      .setMaxPrecision(19);

    metaDataType.computeIfAbsent(Types.SMALLINT, SqlMetaDataType::new)
      .setSqlName("smallint")
      .setMaxPrecision(5);

    metaDataType.computeIfAbsent(Types.TINYINT, SqlMetaDataType::new)
      .setSqlName("tinyint")
      .setMaxPrecision(3);

    /**
     * Numeric
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/decimal-and-numeric-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.NUMERIC, SqlMetaDataType::new)
      .setSqlName("numeric")
      .setMaxPrecision(38)
      .setMaximumScale(38)
      .setDefaultPrecision(18);

    metaDataType.computeIfAbsent(Types.DECIMAL, SqlMetaDataType::new)
      .setSqlName("decimal")
      .setMaxPrecision(38)
      .setDefaultPrecision(18);

    /**
     * Float, Double, Real
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.FLOAT, SqlMetaDataType::new)
      .setSqlName("float")
      .setMaxPrecision(53)
      .setDefaultPrecision(53);

    /**
     * Double = float(53)
     */
    metaDataType.computeIfAbsent(Types.DOUBLE, SqlMetaDataType::new)
      .setSqlName("float")
      .setMaxPrecision(53)
      .setDefaultPrecision(53);

    /**
     *
     * datetime should become datetime2.
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetime-transact-sql?view=sql-server-ver15
     * <p></p>
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetime2-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.TIMESTAMP, SqlMetaDataType::new)
      .setSqlName("datetime2")
      .setDefaultPrecision(7);

    /**
     * TIMESTAMP_WITH_TIMEZONE is known as datetimeoffset
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetimeoffset-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.TIMESTAMP_WITH_TIMEZONE, SqlMetaDataType::new)
      .setSqlName("datetimeoffset")
      .setDefaultPrecision(7);

    metaDataType.computeIfAbsent(SqlServerTypes.DATETIMEOFFSET, i -> metaDataType.get(Types.TIMESTAMP_WITH_TIMEZONE))
      .setSqlJavaClazz(java.sql.Timestamp.class)
      .setDriverTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
      .setDefaultPrecision(7);

    metaDataType.computeIfAbsent(Types.TIME, SqlMetaDataType::new)
      .setSqlName("time")
      .setDefaultPrecision(7);

    /**
     * Boolean
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/bit-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.BOOLEAN, i -> metaDataType.get(Types.BIT))
      .setSqlName("bit");

    /**
     * See
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     */
    metaDataType.computeIfAbsent(SqlTypes.JSON, SqlMetaDataType::new)
      .setSqlName("nvarchar")
      .setDriverTypeCode(Types.NVARCHAR);


    /**
     * See
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     */
    metaDataType.computeIfAbsent(Types.SQLXML, SqlMetaDataType::new)
      .setSqlName("xml");

    return metaDataType;

  }

  @Override
  protected String createDataTypeStatement(ColumnDef columnDef) {

    SqlDataType dataType = sqlConnection.getSqlDataTypeFromSourceDataType(columnDef.getDataType());
    Integer precision = columnDef.getPrecision();
    switch (dataType.getTargetTypeCode()) {
      case Types.TIMESTAMP_WITH_TIMEZONE:
        if (!(precision == null || precision.equals(dataType.getDefaultPrecision()))) {
          return dataType.getSqlName() + "(" + precision + ")";
        }
        return dataType.getSqlName();
      case Types.VARCHAR:
      case Types.NVARCHAR:
        /**
         * The default for varchar is 1 when there is no precision
         * Which means that we got a lot of problem.
         * <p></p>
         * We change that it to make it max and output `max` when the precision is the max
         * <p></p>
         * This has also the effect that JSON takes also the max
         */
        StringBuilder typeStatement = new StringBuilder();
        typeStatement.append(dataType.getSqlName());

        typeStatement.append("(");
        if (precision == null || dataType.getMaxPrecision() != null && columnDef.getPrecision().equals(dataType.getMaxPrecision())) {
          typeStatement.append("max");
        } else {
          typeStatement.append(columnDef.getPrecision());
        }
        typeStatement.append(")");

        return typeStatement.toString();
      default:
        return super.createDataTypeStatement(columnDef);
    }

  }


  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {
    List<SqlMetaColumn> metaColumns = super.getMetaColumns(dataPath);

    metaColumns.forEach(c -> {
        if (c.getTypeCode().equals(SqlServerTypes.DATETIMEOFFSET)) {
          c.setPrecision(c.getScale());
          c.setScale(null);
        }
        /**
         * The driver returns {@link Types.LONGNVARCHAR}
         */
        if (c.getTypeName().equals("xml")) {
          c.setTypeCode(Types.SQLXML);
        }
        /**
         * The driver returns 2147483647 but max is 4000
         */
        if (c.getTypeCode().equals(Types.NVARCHAR)) {
          if (c.getPrecision() > MAX_NVARCHAR_PRECISION) {
            c.setPrecision(MAX_NVARCHAR_PRECISION);
          }
        }
        /**
         * The driver returns 2147483647 but max is 8000
         */
        if (c.getTypeCode().equals(Types.VARCHAR)) {
          if (c.getPrecision() > MAX_VARCHAR_PRECISION) {
            c.setPrecision(MAX_VARCHAR_PRECISION);
          }
        }
      }
    );
    return metaColumns;

  }


  @Override
  protected String createDropTableStatement(SqlDataPath sqlDataPath) {
    if (sqlDataPath.getMediaType() == SqlMediaType.VIEW) {
      // 'DROP VIEW' does not allow specifying the database name as a prefix to the object name.
      return "drop view " + sqlDataPath.toSqlStringPath(2);

    } else {
      return super.createDropTableStatement(sqlDataPath);
    }
  }

  @Override
  public String createInsertStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {
    return super.createInsertStatementWithBindVariables(transferSourceTarget);
  }

  @Override
  public String createUpsertStatementWithPrintfExpressions(TransferSourceTarget transferSourceTarget) {
    return getMergeStatement(transferSourceTarget, false);
  }


  @Override
  public String createUpsertStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {

    return getMergeStatement(transferSourceTarget, true);

  }

  /**
   * Upsert statement is known as Merge
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/statements/merge-transact-sql?view=sql-server-ver16">...</a>
   */
  private String getMergeStatement(TransferSourceTarget transferSourceTarget, boolean sqlBindFormat) {

    // Original upsert statement are insert and then update on conflict
    // Sql server is match expression on column, meaning that
    // if there is no unique column, we need to switch to insert statement
    List<UniqueKeyDef> targetUniqueKeysFoundInSourceColumns = getTargetUniqueKeysFoundInSourceColumns(transferSourceTarget);
    if (targetUniqueKeysFoundInSourceColumns.isEmpty()) {
      if (sqlBindFormat) {
        return createInsertStatementWithBindVariables(transferSourceTarget);
      }
      return createInsertStatementWithPrintfExpressions(transferSourceTarget);
    }

    /**
     * MERGE INTO target_table AS target
     * USING (SELECT ? AS id, ? AS name, ? AS value) AS source
     * ON (target.id = source.id)
     * WHEN MATCHED THEN
     *     UPDATE SET
     *         target.name = source.name,
     *         target.value = source.value
     * WHEN NOT MATCHED THEN
     *     INSERT (id, name, value)
     *     VALUES (source.id, source.name, source.value);
     */
    transferSourceTarget.checkBeforeInsert();

    RelationDef target = transferSourceTarget.getTargetDataPath().getOrCreateRelationDef();
    final SqlDataPath targetDataPath = (SqlDataPath) target.getDataPath();
    StringBuilder mergeStatement = new StringBuilder();
    String targetAlias = "target";
    mergeStatement.append("merge into ")
      .append(targetDataPath.toSqlStringPath())
      .append(" as ")
      .append(targetAlias);
    String sourceAlias = "source";
    String values = createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, sqlBindFormat, true);
    mergeStatement.append(" using (select ")
      .append(values)
      .append(") as ")
      .append(sourceAlias);

    // on
    mergeStatement.append(" on ");
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

    // Jdbc returns this error:
    // A MERGE statement must be terminated by a semi-colon (;). on batch execution
    mergeStatement.append(";");

    return mergeStatement.toString();
  }

  @Override
  public String createViewStatement(SqlDataPath dataPath) {

    if (dataPath.getMediaType() == SqlMediaType.SCRIPT) {
      String query = createOrGetQuery(dataPath);
      // A view in sql server have only one name
      // Otherwise, error !
      // 'CREATE/ALTER VIEW' does not allow specifying the database name as a prefix to the object name.
      String viewName = this.createQuotedName(dataPath.getLogicalName());
      return "create view " + viewName + " as " + query;
    }
    /**
     * We need a name for the view
     * A view is just a stored query
     */
    throw new UnsupportedOperationException("A create view statement is only support for a query data resource");


  }

  /**
   * Truncate does not support multiple table as the standard/postgres does
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/statements/truncate-table-transact-sql?view=sql-server-ver16">...</a>
   */
  protected List<String> createTruncateStatement(List<SqlDataPath> dataPaths) {


    return dataPaths.stream()
      .map(d -> "truncate table " + d.toSqlStringPath())
      .collect(Collectors.toList());

  }

}
