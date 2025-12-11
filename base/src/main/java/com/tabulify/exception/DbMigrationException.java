package com.tabulify.exception;

/**
 *
 */
public class DbMigrationException extends Exception {

  public DbMigrationException() {
    super();
  }

  public DbMigrationException(String s) {
    super(s);
  }

  public DbMigrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
