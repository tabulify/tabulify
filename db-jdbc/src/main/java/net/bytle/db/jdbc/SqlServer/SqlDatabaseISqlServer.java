package net.bytle.db.jdbc.SqlServer;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataSystemSql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gerard on 28-11-2015.
 * <p>
 * Tracing
 * https://docs.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation?view=sql-server-2017
 * Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
 * logger.setLevel(Level.FINE);
 */
public class SqlDatabaseISqlServer extends JdbcDataStoreExtension {

  private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<Integer, DataTypeDatabase>();

  static {
    dataTypeDatabaseSet.put(SqlServerDbNumericType.TYPE_CODE, new SqlServerDbNumericType());
    dataTypeDatabaseSet.put(SqlServerDbIntegerType.TYPE_CODE, new SqlServerDbIntegerType());
    dataTypeDatabaseSet.put(SqlServerDbClobType.TYPE_CODE, new SqlServerDbClobType());
    dataTypeDatabaseSet.put(SqlServerDbTimestampType.TYPE_CODE, new SqlServerDbTimestampType());
    dataTypeDatabaseSet.put(SqlServerDbDecimalType.TYPE_CODE, new SqlServerDbDecimalType());
    dataTypeDatabaseSet.put(SlqServerDbSmallIntType.TYPE_CODE, new SlqServerDbSmallIntType());
  }

  public SqlDatabaseISqlServer(JdbcDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }


  @Override
  public DataTypeDatabase dataTypeOf(Integer typeCode) {
    return dataTypeDatabaseSet.get(typeCode);
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
