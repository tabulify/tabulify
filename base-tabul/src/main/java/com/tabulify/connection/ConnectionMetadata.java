package com.tabulify.connection;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;

import static com.tabulify.conf.Origin.DEFAULT;

/**
 * A class that has all built-in connection attributes
 */
public class ConnectionMetadata {


  /**
   * The connection
   */
  private final Connection connection;


  public ConnectionMetadata(Connection connection) {
    this.connection = connection;
  }


  public ConnectionMetadata setBooleanDataType(ConnectionAttValueBooleanDataType connectionAttValueBooleanDataType) {
    try {
      Attribute attribute = this.connection.getTabular()
        .getVault()
        .createAttribute(
          ConnectionAttributeEnumBase.BOOLEAN_DATA_TYPE,
          connectionAttValueBooleanDataType,
          Origin.DEFAULT
        );
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Integer getMaxNamesInPath() {
    return Integer.MAX_VALUE;
  }

  public ConnectionMetadata setDateDataType(ConnectionAttValueTimeDataType dateDataType) {

    try {
      Attribute attribute = this.connection.getTabular().getVault().createAttribute(ConnectionAttributeEnumBase.DATE_DATA_TYPE, dateDataType, DEFAULT);
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


  }

  public ConnectionMetadata setTimestampDataType(ConnectionAttValueTimeDataType timestampDataType) {
    try {
      Attribute attribute = this.connection.getTabular().getVault().createAttribute(ConnectionAttributeEnumBase.TIMESTAMP_DATA_TYPE, timestampDataType, DEFAULT);
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public ConnectionAttValueTimeDataType getDateDataTypeOrDefault() {

    // date has already a default
    return (ConnectionAttValueTimeDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.DATE_DATA_TYPE).getValueOrDefault();

  }

  public ConnectionAttValueTimeDataType getTimestampDataType() {

    // The TIMESTAMP_DATA_TYPE data type has already a default, it should not happen
    return (ConnectionAttValueTimeDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.TIMESTAMP_DATA_TYPE).getValueOrDefault();


  }

  public ConnectionAttValueTimeDataType getTimeDataType() {

    // The TIME_DATA_TYPE data type has already a default
    return (ConnectionAttValueTimeDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.TIME_DATA_TYPE).getValueOrDefault();


  }

  public ConnectionMetadata setTimeDataType(ConnectionAttValueTimeDataType connectionAttValueTimeDataType) {
    try {
      Attribute attribute = this.connection.getTabular().getVault().createAttribute(ConnectionAttributeEnumBase.TIME_DATA_TYPE, connectionAttValueTimeDataType, DEFAULT);
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConnectionAttValueBooleanDataType getBooleanDataType() {

    // The BOOLEAN_DATA_TYPE data type has already a default
    return (ConnectionAttValueBooleanDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.BOOLEAN_DATA_TYPE).getValueOrDefault();


  }

  /**
   * @return The number of thread that can be created against the data store
   */
  public Integer getMaxWriterConnection() {
    return 100;
  }

  public Connection getConnection() {
    return connection;
  }



}
