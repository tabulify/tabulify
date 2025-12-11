package com.tabulify.fs.sql;

import com.tabulify.fs.Fs;
import com.tabulify.type.Strings;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class SqlQuery {

  /**
   * The SQL select words
   */
  protected static final String SELECT_WORD = "select";
  protected static final String WITH_WORD = "with";
  protected static final String FROM_WORD = "from";
  protected static final String AS_WORD = "as";

  private static final List<String> queryFirstWords = Arrays.asList(SELECT_WORD, WITH_WORD);
  public static final String SPACE_SEPARATOR = " ";

  private final String query;


  public SqlQuery(String query) {
    if (query==null){
      throw new IllegalStateException("To build a query object, the query should not be null");
    }
    /**
     * The possible separator between words
     * are normalized to space
     */
    this.query = query
      .replace("\t", SPACE_SEPARATOR)
      .replace("\r\n", SPACE_SEPARATOR)
      .replace("\n", SPACE_SEPARATOR)
      .trim();
  }

  public static SqlQuery createFromString(String query) {
    return new SqlQuery(query);
  }

  /**
   * @return true if the string is a SQL query (ie start with the 'select' or `with` word
   */
  public Boolean isQuery() {


    int sepIndex = query.indexOf(" ");
    if (sepIndex == -1) {
      return false;
    }
    String firstWord = query.substring(0, sepIndex).toLowerCase();

    return queryFirstWords.contains(firstWord);

  }


  /**
   * @param path the path location of the query
   * @return the first query of a file
   */
  public static SqlQuery createFromPath(Path path) throws NoSuchFileException {

    String query = Fs.getFileContent(path);
    return new SqlQuery(query);

  }

  public List<String> extractColumnNames() {
    if (this.query == null) {
      throw new IllegalStateException("The Query string is null");
    }
    if (!isQuery()) {
      throw new IllegalStateException("The Query string is not a sql query (" + Strings.createFromString(this.query).onOneLine().toString());
    }
    SqlQueryColumnIdentifierExtractor sqlQueryColumnIdentifierExtractor = new SqlQueryColumnIdentifierExtractor(this);
    return sqlQueryColumnIdentifierExtractor.extractColumnIdentifiers();
  }

  @Override
  public String toString() {
    return query;
  }

  public SqlQueryColumnIdentifierExtractor createColumnIdentifierExtractor() {
    return new SqlQueryColumnIdentifierExtractor(this);
  }

  /**
   * Delete the order by
   * Sql Server does not want any order by in a view
   */
  public String toStringWithoutOrderBy() {

    // Convert to uppercase to find the position case-insensitively
    String upperInput = query.toUpperCase();
    String searchString = "ORDER BY";

    // Find the first occurrence of "ORDER BY" (case-insensitive)
    int index = upperInput.indexOf(searchString);

    // If "ORDER BY" is found, return substring up to that position
    if (index != -1) {
      return query.substring(0, index);
    }

    return query;

  }
}
