package com.tabulify.postgres;

import com.tabulify.jdbc.*;
import com.tabulify.model.*;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferSourceTargetOrder;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoSchemaException;
import com.tabulify.type.Casts;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PostgresDataSystem extends SqlDataSystem {

  /**
   * Note that `information_schema.columns` is a high level of pg-attribute
   * <a href="https://www.postgresql.org/docs/current/catalog-pg-attribute.html">...</a>
   */
  public static final String GET_TABLE_COLUMNS_SQL = "SELECT\n" +
    "    table_schema,\n" +
    "    table_name,\n" +
    "    column_name,\n" +
    "    data_type,\n" +
    "    character_maximum_length,\n" +
    "    numeric_precision,\n" +
    "    numeric_scale,\n" +
    "    ordinal_position,\n" +
    "    col_description(pgc.oid, ordinal_position) as column_comment,\n" +
    "    CASE\n" +
    "        WHEN column_default LIKE 'nextval%' THEN 'YES'\n" +
    "        ELSE 'NO'\n" +
    "        END as is_autoincrement,\n" +
    "    CASE\n" +
    "        WHEN is_nullable = 'YES' THEN 1   -- columnNullable\n" +
    "        WHEN is_nullable = 'NO' THEN 0    -- columnNoNulls\n" +
    "        ELSE 2                            -- columnNullableUnknown\n" +
    "        END as nullable\n" +
    "FROM information_schema.columns isc\n" +
    "         LEFT JOIN pg_catalog.pg_class pgc ON pgc.relname = isc.table_name\n" +
    "         LEFT JOIN pg_catalog.pg_namespace pgn ON pgn.oid = pgc.relnamespace\n" +
    "    AND pgn.nspname = isc.table_schema\n" +
    "WHERE table_schema = ?\n" +
    "  AND table_name = ?\n" +
    "ORDER BY table_schema, table_name, ordinal_position";

  /**
   * Due to code improvement, retrieving the data directly
   * from Postgres is no more needed
   */
  @SuppressWarnings("FieldCanBeLocal")
  private final boolean useColumnDriverMeta = true;

  public PostgresDataSystem(SqlConnection sqlConnection) {
    super(sqlConnection);
  }


  @Override
  protected String createDataTypeStatement(ColumnDef<?> columnDef) {
    return super.createDataTypeStatement(columnDef);
  }


  /**
   * The driver returns the aliases
   * <a href="https://www.postgresql.org/docs/current/datatype.html">...</a>
   * <a href="https://www.postgresql.org/docs/7.4/datatype.html#DATATYPE-TABLE">...</a>
   */
  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager sqlDataTypeManager) {

    /**
     * Get the type from the JDBC driver
     * There is a lot of error.
     * We could get them from `pg_catalog.pg_type`
     * but this is not straightforward
     * We have corrected what we need.
     * We assume that the name will not change in the future
     */
    super.dataTypeBuildingMain(sqlDataTypeManager);

    /**
     * {@link Connection#createClob()}  is not supported
     * But it can be easily defined as a synonym to the text type
     * https://stackoverflow.com/questions/49963618/postgresql-clob-datatype
     * <p>
     * Passing a clob to a prepared statement is See{@link org.postgresql.jdbc.PgPreparedStatement#setClob(int, Clob)}
     * return the alias float4 ...
     */
    sqlDataTypeManager.addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.CLOB, PostgresDataType.TEXT.toKeyNormalizer());

    sqlDataTypeManager.addJavaClassToTypeRelation(String.class, PostgresDataType.CHARACTER_VARYING);


    // We got money for double
    sqlDataTypeManager
      .addJavaClassToTypeRelation(Double.class, PostgresDataType.DOUBLE_PRECISION);

    /**
     * Timestamp with timezone has a timestamp type code
     */
    sqlDataTypeManager
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE, PostgresDataType.TIMESTAMP_WITH_TIME_ZONE);
    /**
     * Time with timezone has a time type code
     */
    sqlDataTypeManager
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.TIME_WITH_TIME_ZONE, PostgresDataType.TIME_WITH_TIME_ZONE);

    /**
     * A boolean has {@link Types#BIT} type
     */
    sqlDataTypeManager
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.BOOLEAN, PostgresDataType.BOOLEAN);
    // bit has a 1 precision by default
    sqlDataTypeManager.getTypeBuilder(PostgresDataType.BOOLEAN)
      .setMaxPrecision(0);
    /**
     * Postgres has 2 real (real and float4)
     */
    sqlDataTypeManager.addJavaClassToTypeRelation(Float.class, PostgresDataType.REAL);

  }


  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {

    if (useColumnDriverMeta) {
      return super.getMetaColumns(dataPath);
    }

    /**
     * This code was created because the driver returns `bpchar` as name for a character name
     * At the time, it fucked up our test, no more since
     * We still have an inconsistency between the driver type and the column type
     * How to resolve it is a bit of unknown
     */
    List<SqlMetaColumn> sqlMetaColumns = new ArrayList<>();
    try (
      PreparedStatement statement = this.getMetaColumnsStatement(dataPath);
      ResultSet resultSet = statement.executeQuery()
    ) {
      while (resultSet.next()) {

        String columnName = resultSet.getString("column_name");
        SqlMetaColumn meta = SqlMetaColumn.createOf(columnName);
        sqlMetaColumns.add(meta);

        String dataType = resultSet.getString("data_type");
        meta.setTypeName(dataType);

        int charMaxLength = resultSet.getInt("character_maximum_length");
        int numericPrecision = resultSet.getInt("numeric_precision");
        int precision = Math.max(charMaxLength, numericPrecision);

        meta.setColumnSize(precision);


        int numericScale = resultSet.getInt("numeric_scale");
        meta.setDecimalDigits(numericScale);

        meta.setIsNullable(resultSet.getBoolean("nullable"));
        meta.setIsAutoIncrement(resultSet.getBoolean("is_autoincrement"));
        meta.setComment(resultSet.getString("column_comment"));
        meta.setPosition(resultSet.getInt("ordinal_position"));

      }
    } catch (SQLException e) {
      // we don't pass a bigger message to give any context
      // because the data path may be created temporarily
      // The user would see:
      // Error trying to retrieve the meta from ("tmp_tabulify_4fed3943bb14f96aeff44b6d13c4f253"@sqlite)
      // We let the caller gives the good context
      throw new IllegalStateException(e.getMessage(), e);
    }
    return sqlMetaColumns;
  }

  private PreparedStatement getMetaColumnsStatement(SqlDataPath dataPath) throws SQLException {
    PreparedStatement statement = dataPath.getConnection().getCurrentJdbcConnection().prepareStatement(GET_TABLE_COLUMNS_SQL);
    try {
      statement.setString(1, dataPath.getSchema().getName());
    } catch (NoSchemaException e) {
      throw new InternalException("Postgres has a schema, should not fire", e);
    }
    statement.setString(2, dataPath.getName());
    return statement;
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
    // https://www.postgresql.org/docs/current/functions-admin.html
    // pg_total_relation_size operate on tables or indexes
    SqlRequest sqlRequest = SqlRequest.builder()
      .setSql(this.getConnection(), "select pg_total_relation_size('" + sqlDataPath.toSqlStringPath() + "')")
      .build();
    List<List<?>> records = sqlRequest.execute().getRecords();
    if (records.isEmpty()) {
      // may not exist
      return -1L;
    }
    /**
     * {@link SqlDataTypeAnsi#BIGINT} ie Long
     */
    Object sizeAsObject = records.get(0).get(0);
    try {
      return Casts.cast(sizeAsObject, Long.class);
    } catch (CastException e) {
      throw new InternalException("The returned size of the resource (" + dataPath + ") could not be cast to a long. Error:" + e.getMessage(), e);
    }
  }


  @Override
  public Set<SqlDataTypeVendor> getSqlDataTypeVendors() {
    return Set.of(PostgresDataType.values());
  }

  @Override
  public String createUpsertMergeStatementWithParameters(TransferSourceTargetOrder transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, true, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

  /**
   * Create upsert from values statement
   */
  @Override
  public String createUpsertMergeStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, false, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

  @Override
  public SqlTypeKeyUniqueIdentifier getSqlTypeKeyUniqueIdentifier() {
    /**
     * Type code bug
     * JSONB and JSON returns sometimes {@link java.sql.Types#STRUCT}, sometimes {@link java.sql.Types#OTHER}
     *
     * Due to:
     * SELECT typtype
     * FROM pg_catalog.pg_type
     * where typname= 'jsonb'
     * In the driver, we got, b and c
     * b is OTHER (1111) and c is STRUCT (2002)
     * <p>
     * Query:
     * https://github.com/pgjdbc/pgjdbc/blob/1566eed0caeb26108f9df1d28255538767b7676f/pgjdbc/src/main/java/org/postgresql/jdbc/TypeInfoCache.java#L237
     * typtype to jdbc code
     * https://github.com/pgjdbc/pgjdbc/blob/release/42.7.x/pgjdbc/src/main/java/org/postgresql/jdbc/TypeInfoCache.java#L265
     */
    return SqlTypeKeyUniqueIdentifier.NAME_ONLY;
  }
}
