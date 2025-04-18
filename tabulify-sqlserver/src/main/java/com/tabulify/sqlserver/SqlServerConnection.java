package com.tabulify.sqlserver;

import com.tabulify.Tabular;
import net.bytle.type.Variable;
import com.tabulify.jdbc.SqlConnection;

import java.util.Set;

/**
 * Created by gerard on 28-11-2015.
 * <p>
 * Tracing
 * https://docs.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation?view=sql-server-2017
 * Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
 * logger.setLevel(Level.FINE);
 */
public class SqlServerConnection extends SqlConnection {


  public SqlServerConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);
  }

  @Override
  public Set<Variable> getVariables() {
    Set<Variable> properties = super.getVariables();
    // Sql Server
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
    //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
    Variable applicationName;
    try {
      applicationName = getTabular().createVariable("applicationName", " " + this.getName() + " " + getTabular().getName());
    } catch (Exception e) {
      // should not happen
      throw new RuntimeException("Error while adding the application name", e);
    }
    properties.add(applicationName);
    return properties;
  }


  @Override
  public SqlServerDataSystem getDataSystem() {
    return new SqlServerDataSystem(this);
  }

}
