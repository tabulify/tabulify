package net.bytle.type;

public enum KeyCase {

  /**
   * In Handle Case
   * Snake Case with a space separator
   */
  HANDLE,
  /**
   * In hyphen-case
   */
  HYPHEN,
  /**
   * In snake_case
   */
  SNAKE,
  /**
   * In CamelCase
   */
  CAMEL,
  /**
   * For a sql object name
   */
  SQL,
  /**
   * For a file name
   */
  FILE
}
