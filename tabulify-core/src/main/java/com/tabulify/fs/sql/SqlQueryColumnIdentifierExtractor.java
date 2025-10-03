package com.tabulify.fs.sql;

import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parse a query and returns the column identifier
 * The main class is {@link SqlQuery#extractColumnNames()}
 */
public class SqlQueryColumnIdentifierExtractor {

  public static final char QUOTE_CHAR = '"';
  /**
   * The open/close state of a sql block
   */
  private SqlBlockStatus sqlBlockStatus = SqlBlockStatus.CLOSE;

  private SqlSelectState sqlSelectStatus = SqlSelectState.CLOSE;
  private SqlQuoteIdentifierStatus sqlQuoteIdentifierStatus = SqlQuoteIdentifierStatus.CLOSE;

  private final SqlQuery query;

  /**
   * The token founds
   */
  private List<String> tokenFounds = new ArrayList<>();


  /**
   * The column names found
   */
  private List<String> columnNamesFound = new ArrayList<>();
  /**
   * A parsing stopper
   */
  private boolean stopParsing = false;

  /**
   * If this parameter is true, the identifier of a formula column
   * will be the function name and not the whole formula expression
   */
  private boolean functionNameAsIdentifier = false;

  /**
   * Returned identifier are in lowercase ?
   */
  private boolean isIdentifierLowerCase = false;

  public SqlQueryColumnIdentifierExtractor(SqlQuery query) {
    this.query = query;
  }

  /**
   * When running query with formula,
   * the identifier returned will be only the formula name
   * <p>
   * This is the case with Postgres (see query_13.sql)
   * <p>
   * Example:
   * `select avg(ss_quantity) from ...`
   * will return
   * `avg`
   * as column name
   *
   * @param isFunctionNameIdentifier
   * @return
   */
  public SqlQueryColumnIdentifierExtractor setFunctionNameAsIdentifier(boolean isFunctionNameIdentifier) {
    this.functionNameAsIdentifier = isFunctionNameIdentifier;
    return this;
  }

  /**
   * If not quoted, the identifier are lowercase
   * @param b
   * @return
   */
  public SqlQueryColumnIdentifierExtractor setLowerCaseIdentifier(boolean b) {
    this.isIdentifierLowerCase = b;
    return this;
  }


  /**
   * State used when parsing
   */
  enum SqlSelectState {
    CLOSE,
    OPEN, // We enter this state when we are in the main SELECT block (not the select of a WITH block)
  }

  enum SqlAsType {
    WITH_AS, // We enter this state when we have found the AS key word of a WITH statement
    SELECT_AS // We enter this state when we have found the AS key word of a SELECT statement
  }

  enum SqlBlockStatus {
    OPEN, // We enter this state when we encounter a (
    CLOSE // We enter this state when we encounter a )
  }

  /**
   * The quote of identifier
   */
  enum SqlQuoteIdentifierStatus {
    OPEN, // We enter this state when we encounter a ` "` or a letter
    CLOSE // We enter this state when we encounter a `" ` or a space
  }


  /**
   * Return the first column names (or alias) of the given query
   * This is used during a transfer in order to map the column by name
   * and create the load statement (insert, ...)
   * <p>
   * Based on:
   * https://sqlite.org/lang_select.html
   *
   * @return
   */
  public List<String> extractColumnIdentifiers() {


    /**
     * First pass, creation of the token
     * A token is separated by space or between quote
     */
    String columnsExpression = extractMainSelectColumnsExpression();

    /**
     * Split by
     */
    List<String> columnExpressions = splitByColumnExpression(columnsExpression);

    List<String> columnIdentifiers = new ArrayList<>();
    for (String columnExpression : columnExpressions) {
      String columnIdentifier = extractIdentifierFomColumnExpression(columnExpression);

      /**
       * Do we take only the function name
       * of this is a formula expression
       */
      if (this.functionNameAsIdentifier) {
        int indexOpen = columnIdentifier.indexOf('(');
        int indexClose = columnIdentifier.indexOf(')');
        if (indexOpen != -1 && indexClose != -1) {
          columnIdentifier = columnIdentifier.substring(0, indexOpen).trim();
        }
      }

      /**
       * Casing
       * Postgres return the identifier in lowercase
       */
      if (columnIdentifier.charAt(0)!=QUOTE_CHAR && columnIdentifier.charAt(columnIdentifier.length()-1)!=QUOTE_CHAR) {
        if (this.isIdentifierLowerCase) {
          columnIdentifier = columnIdentifier.toLowerCase();
        }
      } else {
        columnIdentifier = columnIdentifier.substring(1,columnIdentifier.length()-1);
      }

      columnIdentifiers.add(columnIdentifier);
    }
    return columnIdentifiers;

  }

  /**
   * Split the column expression
   * ie col1, col2
   * into
   * [col1, col2]
   *
   * @param columnsExpression
   * @return
   */
  private List<String> splitByColumnExpression(String columnsExpression) {
    String trimmedToken = columnsExpression.trim();
    boolean enteredQuote = false;

    /**
     * ie when this is a literal string
     * 'hello ' || ', ' || 'foo'
     */
    boolean enteredLiteralString = false;

    /**
     * A `,` may be in:
     *   * a function signature round(whatever,2)
     *   * or a analytics function rank(partition over , ...)
     * Example: see query_36.sql
     */
    int blockLevel = 0;
    List<String> columnExpressions = new ArrayList<>();
    StringBuilder currentToken = new StringBuilder();
    for (int i = 0; i < trimmedToken.length(); i++) {
      char c = trimmedToken.charAt(i);
      currentToken.append(c);
      switch (c) {
        case QUOTE_CHAR:
          if (!enteredQuote) {
            /**
             * If this is the fist character
             * or the previous character is:
             *   * a space
             *   * or a `,` (ie ,"hallo")
             *   * or a `.` (ie ,"tab le"." col ")
             */
            if (i == 0 || (i > 1 && Arrays.asList(' ', ',', '.').contains(trimmedToken.charAt(i - 1)))) {
              enteredQuote = true;
            }
          } else {
            /**
             * If we are at the end
             * or the next character is:
             *   * a space
             *   * or a point (ie "tab le"."co l")
             */
            if ((i == trimmedToken.length() - 1) || (i < trimmedToken.length() - 1 && Arrays.asList(' ', '.').contains(trimmedToken.charAt(i + 1)))) {
              enteredQuote = false;
            }
          }
          break;
        case '\'':
          /**
           * Case when a comma is inside a string
           * 'hello ' || ', ' || 'foo'
           */
          if (!enteredLiteralString) {
            enteredLiteralString = true;
          } else {
            enteredLiteralString = false;
          }
          break;
        case '(':
        case ')':
          if (c == '(') {
            blockLevel++;
          } else {
            blockLevel--;
          }
          break;
        case ',':
          if (!enteredQuote && !enteredLiteralString && blockLevel == 0) {
            // That's a split
            // Delete the `,` - add it
            currentToken.deleteCharAt(currentToken.length() - 1);
            columnExpressions.add(currentToken.toString().trim());
            currentToken = new StringBuilder();
          }
          break;
      }
    }
    // The last one
    if (currentToken.length() > 0) {
      columnExpressions.add(currentToken.toString().trim());
    }
    return columnExpressions;
  }

  /**
   * The column expression begins when:
   * * a block is not open (ie the count of ( and ) should be zero)
   * * and we find the `select` key word
   * and terminate when:
   * * it finds a `from` keyword
   * <p>
   * If this is a `select` in a `with` statement, the `select` key word
   * is inside a block and therefore the block char count should not be null.
   * In this case, we would continue.
   *
   * @return the main columns expression (ie the columns id that you will find in a result set)
   */
  private String extractMainSelectColumnsExpression() {

    String s = Strings.createFromString(query.toString()).onOneLine().toString().trim();

    /**
     * The token actually being build
     */
    StringBuilder currentBuildToken = new StringBuilder();
    List<String> tokens = new ArrayList<>();
    /**
     * This state is to see if ( ) are from a function signature (inside the columns)
     * or block opening/closing
     */
    boolean enteredSelect = false;
    int blockCount = 0;

    /**
     * A columns expression (select ... from)
     * can have a select between
     * Example:
     * select case when (select count(*)
     * See query_9.sql
     */
    int intraSelectCount = 0;
    /**
     * Because a name may contains space, we need to parse at the character level
     */
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      currentBuildToken.append(c);
      switch (c) {
        case ' ':

          String token = currentBuildToken.toString();
          currentBuildToken = new StringBuilder();

          /**
           * Edge case of the token `from(select`
           */
          int index = token.indexOf('(');
          List<String> subTokens = new ArrayList<>();
          if (index != -1) {
            subTokens.add(token.substring(0, index));
            subTokens.add(token.substring(index));
          } else {
            subTokens.add(token);
          }
          for (String subToken : subTokens) {

            /**
             * contains because `(select ` is valid SQL
             */
            if (subToken.toLowerCase().trim().contains(SqlQuery.SELECT_WORD)) {

              /**
               * Select into the main select ?
               */
              if (enteredSelect) {
                tokens.add(subToken);
                intraSelectCount++;
              }

              /**
               * Enter top select
               */
              if (!enteredSelect && blockCount == 0) {
                enteredSelect = true;
              }

              continue;
            }
            if (enteredSelect) {
              if (subToken.toLowerCase().trim().contains(SqlQuery.FROM_WORD)) {
                if (intraSelectCount == 0) {
                  /**
                   * End
                   */
                  return String.join("", tokens);
                } else {
                  intraSelectCount--;
                  tokens.add(subToken);
                }
              } else {
                tokens.add(subToken);
              }
            }
          }
          break;
        case ')':
        case '(':
          if (!enteredSelect) {
            if (c == ')') {
              blockCount++;
            } else {
              blockCount--;
            }
          }
          break;
      }
    }
    /**
     * This can happen in query such as `select 1`
     * or
     * `select 1 from` (ahah - bad query but yeah)
     */
    if (currentBuildToken.length() > 0) {
      String token = currentBuildToken.toString();
      if (!token.toLowerCase().trim().contains(SqlQuery.FROM_WORD)) {
        tokens.add(token);
      }
    }
    return String.join("", tokens);
  }


  /**
   * Extract the identifier from a column expression
   * * `alias` from  `col alias`
   * * `alias' from `col as alias`
   * * ` a l i a s` from "c o l" as " a l i a s "
   * * ` c o l` from "c o l"
   *
   * The logic is to keep always the last token
   * and design it as identifier
   *
   * It works great except for formulas.
   * Formulas are not really required
   * but we supports some by adding the token if it starts with some character)
   */
  private String extractIdentifierFomColumnExpression(String columnExpression) {

    String trimmedToken = columnExpression.trim();
    boolean enteredQuote = false;

    /**
     * If the token starts with this character
     * the identifier is
     */
    List<Character> formulaTokens = Arrays.asList('(','|', '\'');
    /**
     * Are we in a block signature
     * 0 = no
     */
    int blockLevel = 0;
    String columnIdentifier = null;
    StringBuilder currentToken = new StringBuilder();
    for (int i = 0; i < trimmedToken.length(); i++) {
      char c = trimmedToken.charAt(i);
      switch (c) {
        case '(':
        case ')':
          currentToken.append(c);
          if (c=='('){
            blockLevel++;
          } else {
            blockLevel--;
          }
          break;
        case QUOTE_CHAR:
          /**
           * We always append the quote
           * to be able to determine the casing (lower/upper)
           */
          currentToken.append(c);
          if (!enteredQuote) {
            /**
             * If this is the fist character
             * or the previous character is a space
             * or the previous space is a `.` (ie "ta ble"."co l"
             */
            if (i == 0 || (i > 1 && Arrays.asList(' ', '.').contains(columnExpression.charAt(i - 1)))) {
              enteredQuote = true;
            }
          } else {
            /**
             * If we are at the end
             * or the next character is a space
             * or the next character is a point (ie " ta ble"."co l")
             */
            if ((i == trimmedToken.length() - 1) || (i < trimmedToken.length() - 1 && Arrays.asList(' ', '.').contains(columnExpression.charAt(i + 1)))) {
              enteredQuote = false;
            }
          }
          break;
        case '.':
          // The point may come from a qualified expression
          // ie table.column
          if (!enteredQuote) {
            // if this is the case, we don't take the first part (ie table.)
            currentToken = new StringBuilder();
          }
          break;
        case ' ':
          if (enteredQuote || blockLevel!=0) {
            currentToken.append(c);
          } else {
            if (currentToken.length() > 0) {
              String tokenFound = currentToken.toString();

              if (!tokenFound.toLowerCase().equals(SqlQuery.AS_WORD)) {
                /**
                 * Case when a formula name has a space
                 * ie
                 * `round (`
                 */
                if (formulaTokens.contains(tokenFound.charAt(0))){
                  columnIdentifier = columnIdentifier + ' ' + tokenFound;
                } else {
                  columnIdentifier = tokenFound;
                }
              }

              /**
               * Reset
               */
              currentToken = new StringBuilder();
            }
          }
          break;
        default:
          currentToken.append(c);
          break;
      }
    }
    if (currentToken.length() > 0) {
      String lastTokenFound = currentToken.toString();
      if (formulaTokens.contains(lastTokenFound.charAt(0))){
        columnIdentifier = columnIdentifier + ' '+ lastTokenFound;
      } else {
        columnIdentifier = lastTokenFound;
      }
    }
    return columnIdentifier;
  }


  /**
   * " char
   *
   * @param previous
   * @param next
   */
  private void processQuote(char previous, char next) {
    if (sqlQuoteIdentifierStatus == SqlQuoteIdentifierStatus.CLOSE) {
      if (previous == ' ') {
        sqlQuoteIdentifierStatus = SqlQuoteIdentifierStatus.OPEN;
      }
    } else {
      if (next == ' ') {
        sqlQuoteIdentifierStatus = SqlQuoteIdentifierStatus.CLOSE;
      }
    }
  }


}
