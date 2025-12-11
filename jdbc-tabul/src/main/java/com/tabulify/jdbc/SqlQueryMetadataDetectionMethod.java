package com.tabulify.jdbc;

import com.tabulify.conf.AttributeValue;
import com.tabulify.fs.sql.SqlQueryColumnIdentifierExtractor;
import com.tabulify.stream.SelectStream;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public enum SqlQueryMetadataDetectionMethod implements AttributeValue {


  /**
   * ie {@link SelectStream#getRuntimeRelationDef()}
   * ie {@link ResultSet#getMetaData()}
   */
  RUNTIME("The metadata are taken after query execution"),

  /**
   * Not all JDBC drivers support metadata retrieval without execution
   * We can add a condition that will never be true to avoid actual data retrieval
   * Example: "SELECT id, name, email, created_date FROM users WHERE 1=0";
   * ie {@link PreparedStatement#getMetaData()}
   */
  DESCRIBE("Get the metadata from the query without execution (ie DESCRIBE command)"),

  /**
   * Wrap the select with a false equality (ie 1=0)
   * and executes it
   */
  FALSE_EQUALITY("Get the metadata by adding a false equality condition"),

  /**
   * With {@link SqlQueryColumnIdentifierExtractor}
   */
  PARSING("Parsing the query file (does not work for query with a star)"),
  /**
   * Creation of a temporary view, reading the
   * metadata and deleting it
   * A result set allows multiple column to have the same name
   * but not a view
   */
  TEMPORARY_VIEW("Creating a view and parsing the metadata");

  private final String description;

  SqlQueryMetadataDetectionMethod(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }
}
