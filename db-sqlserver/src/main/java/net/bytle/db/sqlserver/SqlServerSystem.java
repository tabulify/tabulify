package net.bytle.db.sqlserver;

import net.bytle.db.jdbc.JdbcDataSystemSql;
import net.bytle.db.jdbc.SqlDataPath;
import net.bytle.db.jdbc.SqlDataSystem;

public class SqlServerSystem extends SqlDataSystem {

  public SqlServerSystem(SqlServerDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }

  @Override
  protected String truncateStatement(SqlDataPath dataPath) {
    return "truncate from " +
      JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
  }


  @Override
  protected String createDataTypeStatement(String columnTypeName, Integer precision, Integer scale) {
    switch (columnTypeName) {
      case "CLOB":
        // SQL Server introduces a max specifier for varchar,
        // nvarchar, and varbinary data types to allow storage of values as large as 2^31 bytes.
        return "VARCHAR(max)";
      case "INTEGER":
        return "INT";
      case "TIMESTAMP":
        /**
         *
         * The SQL Server driver gives a smalltime but we want a datetime2.
         *
         * Otherwise when going from an Oracle date to a datetime Sql Server we of the following error
         * because the data range is smaller in a smalldatetime than in a datetime2 (ie Oracle Date)
         *
         * Error:
         * The conversion of a datetime2 data type to a smalldatetime data type resulted in an out-of-range value.
         */
        return "DATETIME2";
      default:
        return super.createDataTypeStatement(columnTypeName,precision,scale);
    }
  }


}
