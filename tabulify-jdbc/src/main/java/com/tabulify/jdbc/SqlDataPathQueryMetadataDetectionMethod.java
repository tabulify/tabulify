package com.tabulify.jdbc;

import com.tabulify.fs.sql.SqlQueryColumnIdentifierExtractor;
import com.tabulify.conf.AttributeValue;

enum SqlDataPathQueryMetadataDetectionMethod implements AttributeValue {

  RESULT_SET("After the query has run, via the result set"),
  /**
   * with {@link SqlQueryColumnIdentifierExtractor}
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

  SqlDataPathQueryMetadataDetectionMethod(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }
}
