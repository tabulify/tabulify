package com.tabulify.sqlserver;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.model.SqlDataType;

import java.sql.Types;
import java.util.Properties;

/**
 * Created by gerard on 28-11-2015.
 * <p>
 * Tracing
 * <a href="https://docs.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation?view=sql-server-2017">...</a>
 * Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
 * logger.setLevel(Level.FINE);
 */
public class SqlServerConnection extends SqlConnection {


  public SqlServerConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }

  @Override
  public Properties getDefaultConnectionProperties() {
    // Sql Server
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
    //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
    Properties properties = new Properties();
    properties.put("applicationName", getTabular().toPublicName(getTabular().getName() + "-" + this.getName()));
    return properties;
  }

  @Override
  public SqlServerDataSystem getDataSystem() {
    return new SqlServerDataSystem(this);
  }

  @Override
  public SqlDataType getSqlDataTypeFromSourceDataType(SqlDataType sourceSqlDataType) {
    if (sourceSqlDataType.getTypeCode() == Types.TIME_WITH_TIMEZONE) {
      return this.getSqlDataType(Types.TIME);
    }
    return super.getSqlDataTypeFromSourceDataType(sourceSqlDataType);
  }
}
