package net.bytle.db.fs.sql;

import net.bytle.type.Strings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.bytle.db.fs.sql.SqlPlusLexer.ScanMode.*;


// See SQL Plus Basics (Running SQL Command, PL/SQL Block)
// http://docs.oracle.com/cd/E11882_01/server.112/e16604/ch_four.htm#i1039255
//
// Excerpt of the above documentation:
// Ending a SQL Command
// You can end a SQL command in one of three ways:
//   * with a semicolon (;)
//   * with a slash (/) on a line by itself
//   * with a blank line
//
//
// Running PL/SQL Block
// SQL*Plus treats PL/SQL subprograms in the same manner as SQL commands, except that a semicolon (;)
// or a blank line does not terminate and execute a block.
// Terminate PL/SQL subprograms by entering a period (.) by itself on a new line.
// You can also terminate and execute a PL/SQL subprogram by entering a slash (/) by itself on a new line.
//
// You enter the mode for entering PL/SQL statements when:
//   * You type DECLARE or BEGIN. After you enter PL/SQL mode in this way, type the remainder of your PL/SQL subprogram.
//   * You type a SQL command (such as CREATE PROCEDURE) that creates a stored procedure.
// SQL*Plus stores the subprograms you enter in the SQL buffer. Execute the current subprogram with a RUN or slash (/) command.
// A semicolon (;) is treated as part of the PL/SQL subprogram and will not execute the command.

final public class SqlPlusLexer implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());

  private Path path;
  /**
   * The input stream
   */
  private BufferedReader reader;
  private Set<String> sqlCommandEnd;
  private HashSet<String> sqlWords;
  private HashSet<String> plsqlWords;
  private HashSet<String> plSqlCommandEnd;
  private HashSet<String> storedProcedure;
  private String commentStart;
  private HashSet<String> sqlPlusWords;


  private final String eol = System.getProperty("line.separator");


  private SqlPlusLexer(BufferedReader reader) {
    this.reader = reader;
    init();
  }


  public static SqlPlusLexer createFromInputStream(InputStream inputStream) {
    return new SqlPlusLexer(new BufferedReader(new InputStreamReader(inputStream)));
  }

  void init() {
    // The characters that end a sql statement on a new Line
    this.sqlCommandEnd = new HashSet<>(Arrays.asList(";", "/", ""));
    // The characters that end a plsql statement on a new Line
    this.plSqlCommandEnd = new HashSet<>(Arrays.asList(".", "/"));
    // The characters that start a comment
    this.commentStart = "--";

    // The Sql Words
    //TODO: incorporate https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getSQLKeywords()
    this.sqlWords = new HashSet<>(Arrays.asList(
      "ANALYZE",
      "ALTER",
      "AUDIT",
      "CALL",
      "COMMIT",
      "COMMENT",
      "CREATE",
      "DELETE",
      "DISASSOCIATE",
      "DROP",
      "EXPLAIN",
      "FLASHBACK",
      "GRANT",
      "INSERT",
      "LOCK",
      "MERGE",
      "NO_AUDIT",
      "PURGE",
      "RENAME",
      "REVOKE",
      "ROLLBACK",
      "SAVEPOINT",
      "SELECT",
      "SET",
      "TRUNCATE",
      "UPDATE",
      "WITH"
    ));

    // The Sql Plus Words that are also Sql Words and that  must not be executed
    this.sqlPlusWords = new HashSet<>(Collections.singletonList(
      "SET"
    ));

    // The Words where Sql Plus will enter in PLSQL Mode
    this.plsqlWords = new HashSet<>(Arrays.asList(
      "DECLARE",
      "BEGIN"
    ));

    // The following stored procedure sql statement will make Sql plus
    // to enter in PLSQL Mode
    this.storedProcedure = new HashSet<>(Arrays.asList(
      "CREATE FUNCTION",
      "CREATE LIBRARY",
      "CREATE LIBRARY",
      "CREATE PACKAGE",
      "CREATE PACKAGE BODY",
      "CREATE PROCEDURE",
      "CREATE TRIGGER",
      "CREATE TYPE"
    ));
  }

  public static SqlPlusLexer createFromString(String script) {
    return new SqlPlusLexer(new BufferedReader(new StringReader(script)));
  }

  public static SqlPlusLexer createFromPath(Path path) {
    try {
      return new SqlPlusLexer(Files.newBufferedReader(path, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the next token.
   * <p/>
   *
   * @return the next token found, if the token is null, it means that we are at the end of the file.
   * @throws java.io.IOException on stream access error
   */
  SqlPlusToken nextToken() throws IOException {

    SqlPlusToken token = null;

    ScanMode scanMode = UNKNOWN_MODE;

    while (true) {

      String line = reader.readLine();

      // EOF
      if (line == null) {
        if (token == null) {
          return null;
        } else {
          return token;
        }
      }

      // Create Token if not a blank line
      String trimLine = line.trim();
      if (token == null) {
        if (!trimLine.equals("")) {
          token = new SqlPlusToken();
        } else {
          // Blank line
          continue;
        }

      }


      // End of Command for a line
      switch (scanMode) {
        case SQL_MODE:
          if (sqlCommandEnd.contains(trimLine)) {
            return token;
          }
          break;
        case PLSQL_MODE:
          if (plSqlCommandEnd.contains(trimLine)) {
            return token;
          }
          break;
        default:
          // Blank line
          if (trimLine.equals("")) {
            continue;
          }
          break;
      }

      // Category definition
      if (token.category == null) {

        // Is it a comment
        if (trimLine.substring(0, 2).equals(commentStart)) {

          // Comment
          token.category = SqlPlusToken.Category.COMMENT;
          token.content.append(line);
          token.content.append(eol);
          return token;

        } else {

          // Statement
          String[] words = trimLine.split(" ");
          String firstWord = words[0].toUpperCase();

          if (sqlWords.contains(firstWord)) {
            // Check if it's not also a SqlPlus word
            if (sqlPlusWords.contains(firstWord)) {
              token.category = SqlPlusToken.Category.SQLPLUS_STATEMENT;
              scanMode = SQL_MODE;
            } else {
              // Check if it's not a stored Procedure
              if (firstWord.equals("CREATE")) {
                String firstTwoWords = firstWord + " " + words[1].toUpperCase();
                if (!storedProcedure.contains(firstTwoWords)) {
                  token.category = SqlPlusToken.Category.SQL_STATEMENT;
                  scanMode = SQL_MODE;
                } else {
                  token.category = SqlPlusToken.Category.PLSQL_STATEMENT;
                  scanMode = PLSQL_MODE;
                }
              } else {
                token.category = SqlPlusToken.Category.SQL_STATEMENT;
                scanMode = SQL_MODE;
              }
            }
          } else if (plsqlWords.contains(firstWord)) {
            token.category = SqlPlusToken.Category.PLSQL_STATEMENT;
            scanMode = PLSQL_MODE;
          } else {
            token.category = SqlPlusToken.Category.SQLPLUS_STATEMENT;
            scanMode = SQL_MODE;
          }

        }

      }


      if (scanMode == SQL_MODE) {
        // A semicolon at the end of a line can end a sql command
        String lastCharacter = trimLine.substring(trimLine.length() - 1, trimLine.length());
        if (lastCharacter.equals(";")) {
          // Add the content and suppress the semicolon
          token.content.append(line.substring(0, Strings.createFromString(line).rtrim().toString().length() - 1));
          return token;
          // Otherwise, add the content and continue
        } else {
          token.content.append(line);
          token.content.append(eol);
        }
      } else {
        token.content.append(line);
        token.content.append(eol);
      }
    }
  }

  public List<String> getSqlStatements() {
    return getSqlPlusTokens().
      stream()
      .filter(t -> t.getCategory().equals(SqlPlusToken.Category.SQL_STATEMENT))
      .map(SqlPlusToken::getContent)
      .collect(Collectors.toList());
  }

  public List<SqlPlusToken> getToken() {
    return getSqlPlusTokens();
  }


  enum ScanMode {

    /**
     * Sql and Sqlplus Statement
     */
    SQL_MODE,

    /**
     * PlSql Statement
     */
    PLSQL_MODE,

    /**
     * Comment mode
     */
    COMMENT_MODE,

    /**
     * Default mode to start the scan
     */
    UNKNOWN_MODE,


  }


  /**
   * Closes the input stream resources.
   */
  public void close() {
    try {
      reader.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<SqlPlusToken> getSqlPlusTokens() {

    List<SqlPlusToken> sqlPlusTokens = new ArrayList<SqlPlusToken>();
    while (true) {
      SqlPlusToken sqlPlusToken = null;
      try {
        sqlPlusToken = this.nextToken();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (sqlPlusToken == null) {
        break;
      } else {
        sqlPlusTokens.add(sqlPlusToken);
      }
    }
    return sqlPlusTokens;
  }


}
