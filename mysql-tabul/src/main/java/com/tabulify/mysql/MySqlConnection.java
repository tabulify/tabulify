package com.tabulify.mysql;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.exception.NoCatalogException;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class MySqlConnection extends SqlConnection {


  private MySqlMetadata mySqlMetadata;
  private MySqlDataSystem mySqlDataSystem;

  public MySqlConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);

    // Set a max default precision on mysql
    // To avoid the Row size too large errors
    // Row size too large. The maximum row size for the used table type, not counting BLOBs, is 65535. This includes storage overhead, check the manual. You have to change some columns to TEXT or BLOBs
    // Because most URL are below 2000, we can create 35 columns with this limit
    // Another solutions would have been to use TEXT and BLOB columns because they don't count toward the row size limit because MySQL stores them separately from the main table data.
    // but yeah
    Integer valueOrDefault = this.sqlDataTypeManager.getDefaultVarcharLength();
    if (valueOrDefault == 0) {
      this.getAttribute(ConnectionAttributeEnumBase.VARCHAR_DEFAULT_PRECISION)
        .setPlainValue(MySqlVendorTypes.VARCHAR.getDefaultPrecision());
    }

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
  public Map<String, Object> getDefaultNativeDriverAttributes() {
    Map<String, Object> connectionProperties = new HashMap<>();

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
  public SqlDataType<?> getSqlDataTypeFromSourceColumn(ColumnDef<?> columnDef) {
    /**
     * Ref: https://dev.mysql.com/doc/refman/8.4/en/other-vendor-data-types.html
     */
    SqlDataType<?> sourceSqlDataType = columnDef.getDataType();
    /**
     * Timezone does not exist
     * MySQL converts TIMESTAMP values from the current time zone to UTC for storage, and back from UTC to the current time zone for retrieval.
     * (This does not occur for other types such as DATETIME.)
     * https://dev.mysql.com/doc/refman/8.4/en/datetime.html
     */
    if (sourceSqlDataType.getVendorTypeNumber() == Types.TIMESTAMP_WITH_TIMEZONE) {
      return this.getSqlDataType(SqlDataTypeAnsi.TIMESTAMP);
    }
    if (sourceSqlDataType.getVendorTypeNumber() == Types.TIME_WITH_TIMEZONE) {
      return this.getSqlDataType(SqlDataTypeAnsi.TIME);
    }
    /**
     * National Varchar does not exist
     * All text are Unicode, no distinction between National data type
     * and normal data type
     * nvarchar = varchar
     * nchar = char
     * https://dev.mysql.com/doc/refman/5.6/en/string-type-syntax.html
     */
    if (sourceSqlDataType.getVendorTypeNumber() == Types.NVARCHAR) {
      return this.getSqlDataType(SqlDataTypeAnsi.CHARACTER_VARYING);
    }
    if (sourceSqlDataType.getVendorTypeNumber() == Types.NCHAR) {
      return this.getSqlDataType(SqlDataTypeAnsi.CHARACTER);
    }
    // no xml type can be loaded into a text field
    if (sourceSqlDataType.getVendorTypeNumber() == Types.SQLXML) {
      return this.getSqlDataType(MySqlVendorTypes.TEXT);
    }
    // no clob type can be loaded into a text field
    if (sourceSqlDataType.getVendorTypeNumber() == Types.CLOB || sourceSqlDataType.getVendorTypeNumber() == Types.NCLOB) {
      return this.getSqlDataType(MySqlVendorTypes.MEDIUMTEXT);
    }

    return super.getSqlDataTypeFromSourceColumn(columnDef);
  }
}
