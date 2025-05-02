package com.tabulify.connection;

import com.tabulify.conf.Attribute;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;

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
        .createAttribute(
          ConnectionAttributeEnumBase.BOOLEAN_DATA_TYPE,
          connectionAttValueBooleanDataType
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
      Attribute attribute = this.connection.getTabular().createAttribute(ConnectionAttributeEnumBase.DATE_DATA_TYPE, dateDataType);
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


  }

  public ConnectionMetadata setTimestampDataType(ConnectionAttValueTimeDataType timestampDataType) {
    try {
      Attribute attribute = this.connection.getTabular().createAttribute(ConnectionAttributeEnumBase.TIMESTAMP_DATA_TYPE, timestampDataType);
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public ConnectionAttValueTimeDataType getDateDataTypeOrDefault() {


    try {
      return (ConnectionAttValueTimeDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.DATE_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The date data type has already a default, it should not happen", e);
    }


  }

  public ConnectionAttValueTimeDataType getTimestampDataType() {

    try {
      return (ConnectionAttValueTimeDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.TIMESTAMP_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The TIMESTAMP_DATA_TYPE data type has already a default, it should not happen", e);
    }


  }

  public ConnectionAttValueTimeDataType getTimeDataType() {

    try {
      return (ConnectionAttValueTimeDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.TIME_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The TIME_DATA_TYPE data type has already a default, it should not happen");
    }

  }

  public ConnectionMetadata setTimeDataType(ConnectionAttValueTimeDataType connectionAttValueTimeDataType) {
    try {
      Attribute attribute = this.connection.getTabular().createAttribute(ConnectionAttributeEnumBase.TIME_DATA_TYPE, connectionAttValueTimeDataType);
      this.connection.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConnectionAttValueBooleanDataType getBooleanDataType() {

    try {
      return (ConnectionAttValueBooleanDataType) this.connection.getAttribute(ConnectionAttributeEnumBase.BOOLEAN_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The BOOLEAN_DATA_TYPE data type has already a default, it should not happen");
    }

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
