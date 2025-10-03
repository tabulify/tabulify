package com.tabulify.sqlserver;

import com.tabulify.fs.sql.SqlQuery;
import com.tabulify.jdbc.*;
import com.tabulify.model.*;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferSourceTargetOrder;
import net.bytle.exception.InternalException;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SqlServerDataSystem extends SqlDataSystem {

  public static final int MAX_NVARCHAR_PRECISION = 4000;


  public static final List<String> ALLOWED_VIEW_CLAUSE_WITH_ORDER_BY = Arrays.asList("TOP", "OFFSET", "FOR XML");
  /**
   * 38 comes from the driver and from the doc, ie you the maximum number would have 38 digits
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/decimal-and-numeric-transact-sql?view=sql-server-ver17#p-precision">...</a>
   * The precision must be a value from 1 through the maximum precision of 38. The default precision is 18.
   */
  public static final int NUMERIC_DECIMAL_MAX_PRECISION = 38;
  public static final int NUMERIC_DECIMAL_PRECISION_DEFAULT = 18;

  /**
   * Max 24 digits
   */
  public static final int REAL_MAX_PRECISION = 24;
  /**
   * 10 digits (yyyy-mm-dd)
   */
  public static final int DATE_MAX_PRECISION = 10;


  public SqlServerDataSystem(SqlServerConnection jdbcDataStore) {
    super(jdbcDataStore);
  }

  /**
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/statements/truncate-table-transact-sql?view=sql-server-ver15">...</a>
   */
  @Override
  public void truncate(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {
    super.truncate(dataPaths, dropAttributes);
  }

  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager typeManager) {

    super.dataTypeBuildingMain(typeManager);


    /**
     * RowVersion is not part of the driver
     * We add it for info
     */
    typeManager.createTypeBuilder(SqlServerTypes.ROWVERSION);

    /**
     * Max precision given by the driver was 27???
     */
    typeManager.getTypeBuilder(SqlServerTypes.DATETIME2)
      .setMaxPrecision(SqlServerTypes.DATETIME2.getMaxPrecision());


    /**
     * CLOB, Text is deprecated for varchar(max)
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     * Clob can be seen as text that comes back as long varchar
     * No support for CLOB natively, we send back a varchar(max) as advised here
     * https://learn.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types?view=sql-server-ver17#blob-and-clob-and-nclob-data-types
     */
    typeManager.addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.CLOB, SqlServerTypes.VARCHAR);


    /**
     * Boolean
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/bit-transact-sql?view=sql-server-ver15
     * There is no boolean data type
     * It's a bit(1)
     * <a href="https://docs.microsoft.com/en-us/sql/t-sql/data-types/bit-transact-sql?view=sql-server-ver15">...</a>
     * <a href="https://learn.microsoft.com/en-us/sql/connect/jdbc/using-basic-data-types">...</a>
     * Alias to bit works because the default precision is 1
     */
    typeManager.addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.BOOLEAN, SqlDataTypeAnsi.BIT);

    /**
     * A float is a double in sql server
     */
    typeManager.addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.FLOAT, SqlServerTypes.FLOAT);

    /**
     * Timestamp to datetime2 (datetime exists also but is not preferred)
     */
    typeManager.addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.TIMESTAMP, SqlServerTypes.DATETIME2);

    /**
     * Priority to numeric (vs decimal)
     */
    typeManager.addJavaClassToTypeRelation(BigDecimal.class, SqlDataTypeAnsi.NUMERIC);

    /**
     * Lala.. sql_identifier is also a string
     */
    typeManager.addJavaClassToTypeRelation(String.class, SqlServerTypes.VARCHAR);

    /**
     * Json (Only in SQL Server 2025 (17.x) Preview.
     */
    SqlDataType.SqlDataTypeBuilder<?> jsonType = typeManager.getTypeBuilder(SqlDataTypeAnsi.JSON.toKeyNormalizer());
    if (jsonType == null) {
      typeManager.addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.JSON, SqlServerTypes.VARCHAR);
    }


  }

  @Override
  public SqlTypeKeyUniqueIdentifier getSqlTypeKeyUniqueIdentifier() {
    return SqlTypeKeyUniqueIdentifier.NAME_ONLY;
  }

  @Override
  protected String createDataTypeStatement(ColumnDef<?> columnDef) {

    SqlDataType<?> dataType = sqlConnection.getSqlDataTypeFromSourceColumn(columnDef);
    int precision = columnDef.getPrecision();
    switch (dataType.getVendorTypeNumber()) {
      case Types.VARCHAR:
      case Types.NVARCHAR:
        /**
         * The default for varchar is 1 when there is no precision
         * Which means that we got nvarchar(1) and a lot of problem when loading unknown length data.
         * <p>
         * We change that it to make it max and output `max` when the precision is the max
         * <p>
         * This has also the effect that JSON takes also the max
         * <p>
         * Ref:
         * When n isn't specified in a data definition or variable declaration statement, the default length is 1. If n isn't specified when using the CAST and CONVERT functions, the default length is 30.
         * https://learn.microsoft.com/en-us/sql/t-sql/data-types/char-and-varchar-transact-sql
         * https://learn.microsoft.com/en-us/sql/t-sql/data-types/nchar-and-nvarchar-transact-sql
         */
        StringBuilder typeStatement = new StringBuilder();
        typeStatement.append(dataType.toKeyNormalizer());

        typeStatement.append("(");
        if (precision == 0) {
          int defaultPrecision = dataType.getDefaultPrecision();
          if (defaultPrecision == 0) {
            // maximum storage size is 2^31-1 characters (2 GB)
            typeStatement.append("max");
          } else {
            typeStatement.append(defaultPrecision);
          }
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
  protected List<String> createDropStatement(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {

    SqlMediaType enumObjectType = sqlDataPaths.get(0).getMediaType();
    SqlDropStatement.DropStatementBuilder sqlDropStatement = SqlDropStatement.builder()
      .setType(enumObjectType)
      .setIfExistsSupported(true)
      .setMultipleSqlObjectSupported(true)
      // cascade is not supported at all (even on schema)
      .setIsCascadeSupported(false);

    switch (enumObjectType) {
      case SCHEMA:
        // 'DROP SCHEMA' does not allow specifying the schema and database name as a prefix to the object name.
        // https://learn.microsoft.com/en-us/sql/t-sql/statements/drop-schema-transact-sql
        sqlDropStatement.setMaximumNamePart(1);
        break;
      case TABLE:
        // 'DROP TABLE' allows all 3 parts
        // https://learn.microsoft.com/en-us/sql/t-sql/statements/drop-table-transact-sql?view=sql-server-ver17
        sqlDropStatement.setMaximumNamePart(3);
        break;
      case VIEW:
      default:
        // 'DROP VIEW' does not allow specifying the database name as a prefix to the object name.
        // https://learn.microsoft.com/en-us/sql/t-sql/statements/drop-view-transact-sql
        sqlDropStatement.setMaximumNamePart(2);
        break;

    }

    return sqlDropStatement.build()
      .getStatements(sqlDataPaths, dropAttributes);


  }

  @Override
  public String createInsertStatementWithBindVariables(TransferSourceTargetOrder transferSourceTarget) {
    return super.createInsertStatementWithBindVariables(transferSourceTarget);
  }

  @Override
  public String createUpsertMergeStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return getMergeStatement(transferSourceTarget, false);
  }


  @Override
  public String createUpsertMergeStatementWithParameters(TransferSourceTargetOrder transferSourceTarget) {

    return getMergeStatement(transferSourceTarget, true);

  }

  /**
   * Upsert statement is known as Merge
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/statements/merge-transact-sql?view=sql-server-ver16">...</a>
   */
  private String getMergeStatement(TransferSourceTargetOrder transferSourceTarget, boolean sqlBindFormat) {

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

    // Jdbc returns this error:
    // A MERGE statement must be terminated by a semi-colon (;). on batch execution
    mergeStatement.append(";");

    return mergeStatement.toString();
  }

  @Override
  protected String getViewName(SqlScript sqlScript, SqlDataPath targetDataPath) {

    // A view in sql server have only one name
    // Otherwise, error !
    // 'CREATE/ALTER VIEW' does not allow specifying the database name as a prefix to the object name.
    if (targetDataPath != null) {
      return this.createQuotedName(targetDataPath.getLogicalName());
    }
    return this.createQuotedName(sqlScript.getExecutableDataPath().getLogicalName());

  }


  @Override
  protected String getViewStatement(String viewStatement) {

    for (String allowedClause : ALLOWED_VIEW_CLAUSE_WITH_ORDER_BY) {
      if (viewStatement.contains(allowedClause)) {
        return viewStatement;
      }
    }
    return SqlQuery.createFromString(viewStatement).toStringWithoutOrderBy();

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

  @Override
  public Long getSize(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    /**
     * Table only for now
     */
    if (!
      (
        sqlDataPath.getMediaType() == SqlMediaType.TABLE ||
          sqlDataPath.getMediaType() == SqlMediaType.SYSTEM_TABLE
      )) {
      return -1L;
    }
    // https://learn.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-allocation-units-transact-sql?view=sql-server-ver17#determine-space-used-by-object-and-type-of-an-allocation-unit
    SqlRequest sqlRequest = SqlRequest.builder()
      .setSql(this.getConnection(), "SELECT u.total_pages\n" +
        "FROM sys.allocation_units AS u\n" +
        "         JOIN sys.partitions AS p ON u.container_id = p.hobt_id\n" +
        "         JOIN sys.tables AS t ON p.object_id = t.object_id\n" +
        "         INNER JOIN sys.schemas s ON t.schema_id = s.schema_id\n" +
        "WHERE t.object_id = OBJECT_ID(?)")
      .build();
    sqlRequest
      .addParameter(
        SqlParameter
          .builder()
          .setValue(sqlDataPath.toSqlStringPath())
      );
    try (SelectStream selectStream = sqlRequest.execute().getSelectStreamSafe()) {
      boolean nextRecord = selectStream.next();
      if (!nextRecord) {
        // may not exist
        return -1L;
      }
      return selectStream.getObject(1, Long.class);
    }

  }


  /**
   * Only one name is authorized
   */
  @Override
  public String createSchemaStatement(SqlDataPath dataPath) {

    if (!dataPath.getMediaType().equals(SqlMediaType.SCHEMA)) {
      throw new InternalException("The data path (" + dataPath + ") is not a schema resource but a " + dataPath.getMediaType());
    }

    return "create schema " + dataPath.toSqlStringPath(1);

  }

  @Override
  public void drop(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {
    if (dataPaths.isEmpty()) {
      return;
    }
    DataPath dataPath = dataPaths.get(0);
    if (dataPath.getMediaType() == SqlMediaType.SCHEMA) {
      /**
       * SQL Server does not support the cascade attribute
       * We do it
       */
      if (dropAttributes.contains(DropTruncateAttribute.CASCADE)) {
        if (!Tabulars.exists(dataPath)) {
          return;
        }
        Tabulars.drop(Tabulars.getChildren(dataPath));
      }
    }
    super.drop(dataPaths, dropAttributes);
  }

  @Override
  public Set<SqlDataTypeVendor> getSqlDataTypeVendors() {
    return Set.of(SqlServerTypes.values());
  }

}
