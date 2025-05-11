package com.tabulify.postgres;

import com.tabulify.jdbc.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlTypes;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Map;

public class PostgresDataSystem extends SqlDataSystem {

  protected static final Integer MAX_PRECISION_NUMERIC = 1000;
  protected static final Integer MAX_SCALE_NUMERIC = 1000;
  // https://www.postgresql.org/docs/current/datatype-character.html
  // If specified, the length n must be greater than zero and cannot exceed 10,485,760.
  public static final Integer MAX_PRECISION_VARCHAR = 10485760;

  public PostgresDataSystem(SqlConnection sqlConnection) {
    super(sqlConnection);
  }


  @Override
  protected String createDataTypeStatement(ColumnDef columnDef) {
    return super.createDataTypeStatement(columnDef);
  }


  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {
    List<SqlMetaColumn> columnsMeta = super.getMetaColumns(dataPath);
    columnsMeta.forEach(meta -> {
      switch (meta.getTypeName()) {
        case "timestamptz":
          /**
           * For whatever reason the driver, return a {@link Types#TIMESTAMP}
           */
          meta.setTypeCode(Types.TIMESTAMP_WITH_TIMEZONE);
          break;
        case "timetz":
          /**
           * For whatever reason the driver, return a {@link Types#TIME}
           */
          meta.setTypeCode(Types.TIME_WITH_TIMEZONE);
          break;
        case "bool":
          /**
           * For whatever reason the driver, return a {@link Types#BIT}
           */
          meta.setTypeCode(Types.BOOLEAN);
          break;
        case "text":
          /**
           * CLOB is not supported, we treat CLOB as text
           */
          meta
            .setTypeCode(Types.CLOB)
            .setPrecision(null);
          break;
        case "json":
          /**
           * Json is a special
           */
          meta.setTypeCode(SqlTypes.JSON).setPrecision(null);
          break;
      }
      switch (meta.getTypeCode()) {
        case Types.NUMERIC:
          /**
           * The driver returns 131089
           */
          if (meta.getPrecision() > MAX_PRECISION_NUMERIC) {
            meta.setPrecision(MAX_PRECISION_NUMERIC);
          }
          if (meta.getScale() > MAX_SCALE_NUMERIC) {
            meta.setScale(MAX_SCALE_NUMERIC);
          }
        case Types.CHAR:
        case Types.NCHAR:
        case Types.VARCHAR:
        case Types.NVARCHAR:
          /**
           * The driver returns 2147483647
           */
          if (meta.getPrecision() > MAX_PRECISION_VARCHAR) {
            meta.setPrecision(MAX_PRECISION_VARCHAR);
          }
      }
    });
    return columnsMeta;
  }

  /**
   * The driver returns the alias
   * <a href="https://www.postgresql.org/docs/7.4/datatype.html#DATATYPE-TABLE">...</a>
   *
   */
  @Override
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {

    Map<Integer, SqlMetaDataType> sqlDataTypes = super.getMetaDataTypes();

    // in place of bpchar ("blank-padded char", the internal name of the character data type)
    sqlDataTypes.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setSqlName("char")
      .setDefaultPrecision(1)
      .setMaxPrecision(MAX_PRECISION_VARCHAR);

    // the precision of the driver was not the same than taken from the meta (10485760)
    sqlDataTypes.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setMaxPrecision(MAX_PRECISION_VARCHAR);


    /**
     * {@link Connection#createClob()}  is not supported
     *
     * but to pass a clob to a prepared statement is See{@link org.postgresql.jdbc.PgPreparedStatement#setClob(int, Clob)}
     *
     * The driver return the alias float4
     *
     * Binary (blob) are with the type oid
     * https://www.postgresql.org/docs/7.1/jdbc-lo.html
     */
    sqlDataTypes.computeIfAbsent(Types.CLOB, SqlMetaDataType::new)
      .setSqlName("text")
      .setDriverTypeCode(Types.VARCHAR)
      .setSqlJavaClazz(String.class);

    /**
     * Unicode character strings
     * All character string are unicode in Postgres
     * See https://stackoverflow.com/questions/1245217/what-is-the-postgresql-equivalent-to-sql-server-nvarchar
     * NVARCHAR = VARCHAR then
     */
    sqlDataTypes.computeIfAbsent(Types.NVARCHAR, SqlMetaDataType::new)
      .setSqlName("varchar")
      .setDriverTypeCode(Types.VARCHAR)
      .setMaxPrecision(MAX_PRECISION_VARCHAR)
      .setSqlJavaClazz(String.class);

    sqlDataTypes.computeIfAbsent(Types.NCHAR, SqlMetaDataType::new)
      .setSqlName("char")
      .setDriverTypeCode(Types.CHAR)
      .setMaxPrecision(MAX_PRECISION_VARCHAR)
      .setSqlJavaClazz(String.class);

    // The driver return serial for integer
    sqlDataTypes.computeIfAbsent(Types.INTEGER, SqlMetaDataType::new)
      .setSqlName("integer")
      .setAutoIncrement(false);

    // The driver return oid for bigint
    sqlDataTypes.computeIfAbsent(Types.BIGINT, SqlMetaDataType::new)
      .setSqlName("bigint")
      .setAutoIncrement(false);

    // The driver return the alias int2
    sqlDataTypes.computeIfAbsent(Types.SMALLINT, SqlMetaDataType::new)
      .setSqlName("smallint")
      .setAutoIncrement(false);

    // From https://www.postgresql.org/docs/13/datatype-numeric.html
    // the driver return 1000
    sqlDataTypes.computeIfAbsent(Types.NUMERIC, SqlMetaDataType::new)
      .setMaxPrecision(MAX_PRECISION_NUMERIC)
      .setMaximumScale(MAX_PRECISION_NUMERIC);

    // Alias name for numeric
    // https://www.postgresql.org/docs/7.4/datatype.html#DATATYPE-TABLE
    sqlDataTypes.computeIfAbsent(Types.DECIMAL, s -> sqlDataTypes.get(Types.NUMERIC));

    // From https://www.postgresql.org/docs/13/datatype-numeric.html
    // the driver return the alias float4
    sqlDataTypes.computeIfAbsent(Types.REAL, SqlMetaDataType::new)
      .setSqlName("real")
      .setMaximumScale(16383);

    // Alias for real
    // https://www.postgresql.org/docs/7.4/datatype.html#DATATYPE-TABLE
    sqlDataTypes.computeIfAbsent(Types.FLOAT, s -> sqlDataTypes.get(Types.REAL));

    // This is a "double precision" floating point number which supports 15 digits of mantissa.
    // Driver was returning money
    sqlDataTypes.computeIfAbsent(Types.DOUBLE, SqlMetaDataType::new)
      .setSqlName("double precision");

    // https://www.postgresql.org/docs/9.1/datatype-datetime.html
    // This is a "timestamp" (ie without timezone)
    // Driver was returning timestamptz (ie with tz)
    sqlDataTypes.computeIfAbsent(Types.TIMESTAMP, SqlMetaDataType::new)
      .setSqlName("timestamp")
      .setMaxPrecision(6);

    // https://www.postgresql.org/docs/9.1/datatype-datetime.html
    // Postgres expect the sql name to be lowercase
    sqlDataTypes.computeIfAbsent(Types.TIMESTAMP_WITH_TIMEZONE, SqlMetaDataType::new)
      .setSqlName("timestamp with time zone")
      .setMaxPrecision(6);

    // https://www.postgresql.org/docs/9.1/datatype-datetime.html
    // returns timetz
    sqlDataTypes.computeIfAbsent(Types.TIME, SqlMetaDataType::new)
      .setSqlName("time")
      .setMaxPrecision(6);

    // https://www.postgresql.org/docs/9.1/datatype-datetime.html
    // Postgres expect the sql name to be lowercase and there was no maximum defined
    sqlDataTypes.computeIfAbsent(Types.TIME_WITH_TIMEZONE, SqlMetaDataType::new)
      .setSqlName("time with time zone")
      .setMaxPrecision(6);

    // https://www.postgresql.org/docs/9.1/datatype-boolean.html
    // Postgres expect the sql name to be lowercase
    sqlDataTypes.computeIfAbsent(Types.BOOLEAN, SqlMetaDataType::new)
      .setSqlName("boolean");

    /**
     * https://www.postgresql.org/docs/13/datatype-json.html
     * JSON as type is also working but we just follow the lowercase rule of postgres
     *
     * In the documentation, they talk about
     * a {@link org.postgresql.util.PGobject}
     * but a string for json is working
     */
    sqlDataTypes.computeIfAbsent(SqlTypes.JSON, SqlMetaDataType::new)
      .setSqlName("json")
      .setSqlJavaClazz(String.class)
      .setDriverTypeCode(Types.OTHER);


    return sqlDataTypes;

  }


}
