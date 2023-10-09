package net.bytle.db.connection;

import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.Variable;

/**
 * A class that has all built-in connection attributes
 */
public class ConnectionMetadata {


  /**
   * The connection
   */
  private final Connection connection;

  /**
   * When the date data type is not supported by the data store
   * (ie Sqlite)
   * we store date as sql literal because it's more readable
   * <p>
   * The three fields are respectively for:
   * * DATE
   * * TIMESTAMP
   * * TIME
   */
  static final ConnectionAttValueTimeDataType DEFAULT_DATE_DATA_TYPE = ConnectionAttValueTimeDataType.NATIVE;
  private static final ConnectionAttValueTimeDataType DEFAULT_TIMESTAMP_DATA_TYPE = ConnectionAttValueTimeDataType.NATIVE;
  private static final ConnectionAttValueTimeDataType DEFAULT_TIME_DATA_TYPE = ConnectionAttValueTimeDataType.NATIVE;


  private static final ConnectionAttValueBooleanDataType DEFAULT_BOOLEAN_DATA_TYPE = ConnectionAttValueBooleanDataType.Native;


  public ConnectionMetadata(Connection connection) {
    this.connection = connection;
  }


  public ConnectionMetadata setBooleanDataType(ConnectionAttValueBooleanDataType connectionAttValueBooleanDataType) {
    try {
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttribute.BOOLEAN_DATA_TYPE, connectionAttValueBooleanDataType);
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
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttribute.DATE_DATA_TYPE, dateDataType);
      this.connection.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


  }

  public ConnectionMetadata setTimestampDataType(ConnectionAttValueTimeDataType timestampDataType) {
    try {
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttribute.TIMESTAMP_DATA_TYPE, timestampDataType);
      this.connection.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public ConnectionAttValueTimeDataType getDateDataTypeOrDefault() {


    try {
      return (ConnectionAttValueTimeDataType) this.connection.getVariable(ConnectionAttribute.DATE_DATA_TYPE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The date data type has already a default, it should not happen", e);
    }


  }

  public ConnectionAttValueTimeDataType getTimestampDataType() {

    try {
      return (ConnectionAttValueTimeDataType) this.connection.getVariable(ConnectionAttribute.TIMESTAMP_DATA_TYPE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The TIMESTAMP_DATA_TYPE data type has already a default, it should not happen", e);
    }


  }

  public ConnectionAttValueTimeDataType getTimeDataType() {

    try {
      return (ConnectionAttValueTimeDataType) this.connection.getVariable(ConnectionAttribute.TIME_DATA_TYPE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The TIME_DATA_TYPE data type has already a default, it should not happen");
    }

  }

  public ConnectionMetadata setTimeDataType(ConnectionAttValueTimeDataType connectionAttValueTimeDataType) {
    try {
      Variable variable = this.connection.getTabular().createVariable(ConnectionAttribute.TIME_DATA_TYPE, connectionAttValueTimeDataType);
      this.connection.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConnectionAttValueBooleanDataType getBooleanDataType() {

    try {
      return (ConnectionAttValueBooleanDataType) this.connection.getVariable(ConnectionAttribute.BOOLEAN_DATA_TYPE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
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
