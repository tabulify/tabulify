package com.tabulify.postgres;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;
import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlMediaType;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;

import java.util.function.Supplier;

public class PostgresConnection extends SqlConnection {

  public PostgresConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }


  @Override
  public PostgresDataSystem getDataSystem() {
    return new PostgresDataSystem(this);
  }


  @Override
  protected Supplier<SqlDataPath> getDataPathSupplier(String pathOrName, SqlMediaType mediaType) {
    return () -> new PostgresDataPath(this, pathOrName, mediaType);
  }

  @Override
  public SqlConnectionMetadata getMetadata() {
    return new PostgresFeatures(this);
  }

  @Override
  public SqlDataType<?> getSqlDataTypeFromSourceColumn(ColumnDef<?> columnDef) {

    SqlDataType<?> sourceSqlDataType = columnDef.getDataType();
    if (sourceSqlDataType.getConnection().equals(this)) {
      return sourceSqlDataType;
    }

    /**
     * Doc: http://localhost:8081/postgres-data-type-support-z3adbcca
     */
    switch (sourceSqlDataType.getAnsiType()) {
      case FLOAT:
        /**
         * Sql Float (ie variable precision) does not exist in Postgres
         * Only fixed value exists:
         * - float4 (ie real, single precision)
         * - float8 (ie double precision)
         */
        // Float as sql float does not exist in Postgres (You choose float4 for single precision or float8 for double precision)
        // We take a float8 in case of float
        // https://www.postgresql.org/docs/7.4/datatype.html#DATATYPE-TABLE
        int precision = columnDef.getPrecision();
        if (precision != 0 && precision <= 24) {
          return this.getSqlDataType(PostgresDataType.REAL);
        }
        return this.getSqlDataType(PostgresDataType.DOUBLE_PRECISION);
      case TINYINT:
        /**
         * Tinyint does not exist in Postgres
         */
        return this.getSqlDataType(PostgresDataType.SMALLINT);
      case NATIONAL_CHARACTER_VARYING:
        /**
         * NVARCHAR does not exist in Postgres
         * NVARCHAR and NCHAR does not exist, Postgres stores text in only 1 character set
         * See https://stackoverflow.com/questions/1245217/what-is-the-postgresql-equivalent-to-sql-server-nvarchar
         * https://stackoverflow.com/a/79758631/297420
         */
        return this.getSqlDataType(PostgresDataType.CHARACTER_VARYING);
      case NATIONAL_CHARACTER:
        /**
         * NCHAR does not exist in Postgres
         * NVARCHAR and NCHAR does not exist, Postgres stores text in only 1 character set
         * See https://stackoverflow.com/questions/1245217/what-is-the-postgresql-equivalent-to-sql-server-nvarchar
         * https://stackoverflow.com/a/79758631/297420
         */
        return this.getSqlDataType(PostgresDataType.CHARACTER);
      case DECIMAL:
        /**
         * Postgres create a numeric when using the decimal name
         * ie a `create table` statement with a decimal name will create a column with a numeric name
         */
        return this.getSqlDataType(PostgresDataType.NUMERIC);
      case JSON:
        return this.getSqlDataType(PostgresDataType.JSON);
    }

    return super.getSqlDataTypeFromSourceColumn(columnDef);
  }


}
