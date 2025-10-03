package com.tabulify.fs.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tabulify.fs.sql.SqlLexer.ScanMode.*;

public class SqlLexerTokenizer {
  private final BufferedReader reader;
  private final SqlLexer.SqlPlusLexerBuilder builder;
  private int lineCounter;
  SqlStatement.SqlStatementBuilder nextToken = null;

  public SqlLexerTokenizer(SqlLexer.SqlPlusLexerBuilder builder, BufferedReader bufferedReader) {
    this.reader = bufferedReader;
    this.builder = builder;
    this.lineCounter = 0;
  }

  /**
   * Returns the next token.
   * <p/>
   *
   * @return the next token found, if the token is null, it means that we are at the end of the file.
   */
  boolean next() {

    nextToken = null;
    SqlLexer.ScanMode scanMode = START_MODE;

    while (true) {

      String line;
      try {
        line = reader.readLine();
        lineCounter++;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      // EOF
      if (line == null) {
        return nextToken != null;
      }

      // Create Token if not a blank line
      String trimmedLine = line.trim();
      if (nextToken == null) {
        if (trimmedLine.isEmpty()) {
          // Blank line
          continue;
        }
        nextToken = SqlStatement.builder()
          .setLineNumber(lineCounter);
      }

      // End of Command for a line
      switch (scanMode) {
        case SQL_MODE:
          if (builder.getNewLineEndCommandCharacterSet().contains(trimmedLine)) {
            return true;
          }
          break;
        case PSQL_MODE:
          if (builder.getNewLineEndCommandPSqlCharacterSet().contains(trimmedLine)) {
            /**
             * Postgres case
             */
            if (trimmedLine.equals("$$;")) {
              nextToken.append("$$");
            }
            return true;
          }
          break;
        default:
          // Blank line
          if (trimmedLine.isEmpty()) {
            continue;
          }
          break;
      }

      // Category definition
      if (nextToken.getKind() == null) {

        // Is it a comment ?
        for (String comment : builder.getCommentStrings()) {
          if (trimmedLine.startsWith(comment)) {
            // Comment
            nextToken
              .setKind(SqlStatementKind.SCRIPT_COMMENT)
              .append(line)
              .append(this.builder.getEol());
            return true;
          }
        }


        // Determine the sql key word
        List<String> words = new ArrayList<>();
        for (String word : trimmedLine.split(" ")) {
          // Delete all starts characters non-relevant
          // such as `{` escape character and `?` parameter placeholder
          // Example of case: {? = call upper( ? ) }
          if (words.isEmpty() && (word.startsWith("{") || word.startsWith("?") || word.endsWith("}") || word.endsWith("="))) {
            continue;
          }
          words.add(word);
        }
        SqlStatementKind sqlStatementKind = SqlStatementKind.cast(words);

        // Example of case:
        // {? = call upper( ? ) }
        if (trimmedLine.startsWith("{") && trimmedLine.endsWith("}")) {
          nextToken
            .setKind(sqlStatementKind)
            .append(line)
            .append(this.builder.getEol());
          return true;
        }

        /**
         * Command are on 1 line
         */
        if (sqlStatementKind.getSqlTokenCategory().equals(SqlTokenCategory.COMMAND)) {
          nextToken
            .setKind(sqlStatementKind)
            .append(line)
            .append(this.builder.getEol());
          return true;
        }


        nextToken.setKind(sqlStatementKind);
        switch (sqlStatementKind.getSqlTokenCategory()) {
          case PSQL:
            scanMode = PSQL_MODE;
            break;
          default:
            scanMode = SQL_MODE;
            break;
        }

      }

      // End of command at the end of the line
      // Note: we can have empty line in PSQL Mode in BEGIN/END block)
      if (!trimmedLine.isEmpty()) {

        // A semicolon at the end of a line can end a sql command
        // A // can also
        // It's a SQL and PSQL mode
        Set<String> endOfLines;
        if (scanMode == PSQL_MODE) {
          endOfLines = builder.getEndOfLineEndCommandPsqlCharacterSet();
        } else {
          endOfLines = builder.getEndOfLineEndCommandCharacterSet();
        }
        for (String endLine : endOfLines) {
          if (trimmedLine.endsWith(endLine)) {

            // Add the content and suppress the string (semicolon)
            String trimmedLineWithoutEndCommandChar = trimmedLine.substring(0, trimmedLine.length() - endLine.length());
            // Postgres
            if (endLine.equals("$$;")) {
              trimmedLineWithoutEndCommandChar += "$$";
            }
            nextToken
              .append(trimmedLineWithoutEndCommandChar.trim());
            return true;
          }
        }

      }

      // Continue
      nextToken
        .append(line)
        .append(builder.getEol());

    }
  }

  public SqlStatement getToken() {
    return this.nextToken.build();
  }
}
