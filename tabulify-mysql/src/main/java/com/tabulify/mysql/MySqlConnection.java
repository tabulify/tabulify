package com.tabulify.mysql;

import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.SqlDataType;
import net.bytle.exception.NoCatalogException;
import net.bytle.type.Variable;

import java.sql.Types;
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

  public boolean isPlanetScale() {
    String url = this.getURL();
    return url.contains("psdb.cloud");
  }

  @Override
  public Properties getDefaultConnectionProperties() {
    Properties connectionProperties = new Properties();

    // Should the JDBC driver treat the MySQL type YEAR as a 'java.sql.Date', or as a SHORT?
    // By default, a Date is seen as a year (ie short number)
    // We disable it ...
    // Ref: https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-datetime-types-processing.html#cj-conn-prop_yearIsDateType
    connectionProperties.put("yearIsDateType", false);

    // To avoid: Invalid default value for timestamp field
    // https://stackoverflow.com/questions/9192027/invalid-default-value-for-create-date-timestamp-field
    // https://dev.mysql.com/doc/refman/5.7/en/sql-mode.html#sqlmode_no_zero_date
    connectionProperties.put("sessionVariables", "sql_mode=''");

    return connectionProperties;
  }

  @Override
  public SqlDataType getSqlDataTypeFromSourceDataType(SqlDataType sourceSqlDataType) {
    /**
     * Timezone does not exist
     * MySQL converts TIMESTAMP values from the current time zone to UTC for storage, and back from UTC to the current time zone for retrieval.
     * https://dev.mysql.com/doc/refman/8.4/en/datetime.html
     */
    if (sourceSqlDataType.getTypeCode() == Types.TIMESTAMP_WITH_TIMEZONE) {
      return this.getSqlDataType(Types.TIMESTAMP);
    }
    if (sourceSqlDataType.getTypeCode() == Types.TIME_WITH_TIMEZONE) {
      return this.getSqlDataType(Types.TIME);
    }
    return super.getSqlDataTypeFromSourceDataType(sourceSqlDataType);
  }
}
