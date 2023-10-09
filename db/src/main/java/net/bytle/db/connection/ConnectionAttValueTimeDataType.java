package net.bytle.db.connection;

import net.bytle.type.AttributeValue;

/**
 * The data type of a date in the case
 * that the connection system is not implementing it
 *
 * Ie Used when the database does not supports the SQL DATE and/or TIMESTAMP data type
 * (ie SQLite for instance)
 *
 *
 *
 */
public enum ConnectionAttValueTimeDataType implements AttributeValue {

  SQL_LITERAL("The sql literal representation of a date (ie YYYY-MM-DD) to the millis"),
  EPOCH_MS("Epoch Ms"),
  EPOCH_SEC( "Epoch Sec"),
  EPOCH_DAY("Epoch Day"),
  NATIVE("Sql native type");


  private final String description;

  ConnectionAttValueTimeDataType(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

}
