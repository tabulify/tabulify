package net.bytle.db.sqlserver;

import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataSystemSql;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;

import java.sql.Types;
import java.util.List;

/**
 * Created by gerard on 28-11-2015.
 * <p>
 * Tracing
 * https://docs.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation?view=sql-server-2017
 * Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
 * logger.setLevel(Level.FINE);
 */
public class SqlServerDataStoreExtension extends JdbcDataStoreExtension {


  public SqlServerDataStoreExtension(JdbcDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }

  @Override
  public void updateSqlDataType(SqlDataType sqlDataType) {
    switch (sqlDataType.getTypeCode()) {
      case Types.CLOB:
        sqlDataType.setTypeName("VARCHAR");
        break;
    }
  }

  @Override
  public String getCreateColumnStatement(ColumnDef columnDef) {
    SqlDataType dataType = columnDef.getDataType();
    switch (dataType.getTypeCode()) {
      case Types.CLOB:
        // SQL Server introduces a max specifier for varchar,
        // nvarchar, and varbinary data types to allow storage of values as large as 2^31 bytes.
        return "VARCHAR(max)";
      case Types.INTEGER:
        return "INT";
      case Types.TIMESTAMP:
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
        return null;
    }
  }


  /**
   * Returns statement to create the table
   *
   * @param dataPath
   * @return
   */
  @Override
  public List<String> getCreateTableStatements(JdbcDataPath dataPath) {
    return null;
  }


  @Override
  public String getNormativeSchemaObjectName(String objectName) {
    return "[" + objectName + "]";
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 100;
  }


  @Override
  public String getTruncateStatement(JdbcDataPath dataPath) {
    StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    return truncateStatementBuilder.toString();
  }


}
