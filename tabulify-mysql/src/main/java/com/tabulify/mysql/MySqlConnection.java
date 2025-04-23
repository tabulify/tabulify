package com.tabulify.mysql;

import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import net.bytle.exception.NoCatalogException;
import net.bytle.type.Variable;

import java.util.Properties;

public class MySqlConnection extends SqlConnection {

  private MySqlMetadata mySqlMetadata;
  private MySqlDataSystem mySqlDataSystem;

  public MySqlConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);
  }


  @Override
  public MySqlMetadata getMetadata() {
    if (mySqlMetadata == null) {
      mySqlMetadata = new MySqlMetadata(this);
    }
    return mySqlMetadata;
  }

  @Override
  public SqlDataSystem getDataSystem() {
    if (mySqlDataSystem == null) {
      mySqlDataSystem = new MySqlDataSystem(this);
    }
    return mySqlDataSystem;
  }

  @Override
  public String getCurrentCatalog() throws NoCatalogException {
    throw new NoCatalogException("Catalog is not supported");
  }

  public boolean isPlanetScale(){
    String url = this.getURL();
    return url.contains("psdb.cloud");
  }

  @Override
  public Properties getDefaultConnectionProperties() {
    Properties connectionProperties= new Properties();
    // Should the JDBC driver treat the MySQL type YEAR as a 'java.sql.Date', or as a SHORT?
    // By default, a Date is seen as a year (ie short number)
    // We disable it ...
    // Ref: https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-datetime-types-processing.html#cj-conn-prop_yearIsDateType
    connectionProperties.put("yearIsDateType",false);
    return connectionProperties;
  }

}
