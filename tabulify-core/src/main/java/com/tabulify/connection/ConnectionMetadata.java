package com.tabulify.connection;

import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.type.Variable;

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
      Variable variable = this.connection.getTabular()
        .createVariable(
          ConnectionAttributeBase.BOOLEAN_DATA_TYPE,
          connectionAttValueBooleanDataType
        );
      this.connection.addVariable(variable);
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
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttributeBase.DATE_DATA_TYPE, dateDataType);
      this.connection.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


  }

  public ConnectionMetadata setTimestampDataType(ConnectionAttValueTimeDataType timestampDataType) {
    try {
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttributeBase.TIMESTAMP_DATA_TYPE, timestampDataType);
      this.connection.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public ConnectionAttValueTimeDataType getDateDataTypeOrDefault() {


    try {
      return (ConnectionAttValueTimeDataType) this.connection.getVariable(ConnectionAttributeBase.DATE_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The date data type has already a default, it should not happen", e);
    }


  }

  public ConnectionAttValueTimeDataType getTimestampDataType() {

    try {
      return (ConnectionAttValueTimeDataType) this.connection.getVariable(ConnectionAttributeBase.TIMESTAMP_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The TIMESTAMP_DATA_TYPE data type has already a default, it should not happen", e);
    }


  }

  public ConnectionAttValueTimeDataType getTimeDataType() {

    try {
      return (ConnectionAttValueTimeDataType) this.connection.getVariable(ConnectionAttributeBase.TIME_DATA_TYPE).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("The TIME_DATA_TYPE data type has already a default, it should not happen");
    }

  }

  public ConnectionMetadata setTimeDataType(ConnectionAttValueTimeDataType connectionAttValueTimeDataType) {
    try {
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttributeBase.TIME_DATA_TYPE, connectionAttValueTimeDataType);
      this.connection.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConnectionAttValueBooleanDataType getBooleanDataType() {

    try {
      return (ConnectionAttValueBooleanDataType) this.connection.getVariable(ConnectionAttributeBase.BOOLEAN_DATA_TYPE).getValueOrDefault();
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
