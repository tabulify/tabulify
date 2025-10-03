package com.tabulify.spi;

/**
 * Can we rely on the {@link com.tabulify.model.RelationDef data definition}
 * so that we can make check on them
 */
public enum SchemaType {

  /**
   * Schema Before
   * Data structure is known
   * A not null will be a not null
   * Example: a table in a relational database
   */
  STRICT,
  /**
   * Schema Later
   * Data structure is not precisely known
   * Example:
   * * a sql view will put all column as nullable as they don't inherit constraints (like primary keys or NOT NULL constraints
   * from their underlying tables)
   */
  LOOSE

}
