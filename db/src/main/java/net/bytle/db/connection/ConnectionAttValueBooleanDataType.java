package net.bytle.db.connection;

import net.bytle.type.AttributeValue;

/**
 * The data type of a boolean in the case
 * that the connection system is not implementing it
 *
 * Ie Used when the database does not supports the BOOLEAN data type
 * (ie SQLite for instance)
 *
 *
 */
public enum ConnectionAttValueBooleanDataType implements AttributeValue {

  Binary("0 or 1"),
  Native( "Boolean");

  private final String desc;

  ConnectionAttValueBooleanDataType(String description) {
    this.desc = description;
  }


  @Override
  public String getDescription() {
    return this.desc;
  }
}
