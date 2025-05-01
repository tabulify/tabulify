package com.tabulify.connection;

import net.bytle.type.AttributeValue;

/**
 * The datatype of a date in case
 * the connection system is not implementing it
 * <p></p>
 * Ie Used when the database does not support the SQL DATE and/or TIMESTAMP data type
 * (ie SQLite for instance)
 * When the date data type is not supported by the data store
 * (ie Sqlite)
 * we store date as sql literal because it's more readable
 * <p>
 * The three fields are respectively for:
 * * DATE
 * * TIMESTAMP
 * * TIME
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
