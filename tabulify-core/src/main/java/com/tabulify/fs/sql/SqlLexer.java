package com.tabulify.fs.sql;

import net.bytle.fs.FsTextCharacterSetNotDetected;
import net.bytle.fs.FsTextDetectedCharsetNotSupported;
import net.bytle.fs.FsTextFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;


/**
 * A Sql lexer that returns Sql Statement as token
 * This lexer will not return any error.
 * If a statement is not recognized as SQL, it's categorized as {@link SqlStatementKind#UNKNOWN}
 * <p></p>
 * Historically, it was created to parse SQLPlus script, see the original logic below.
 * <p>
 * See SQL Plus Basics (Running SQL Command, PL/SQL Block)
 * http:docs.oracle.com/cd/E11882_01/server.112/e16604/ch_four.htm#i1039255
 * <p>
 * Excerpt of the above documentation:
 * Ending a SQL Command
 * You can end a SQL command in one of three ways:
 * * with a semicolon (;)
 * * with a slash (/) on a line by itself
 * * with a blank line
 * <p>
 * <p>
 * Running PL/SQL Block
 * SQL*Plus treats PL/SQL subprograms in the same manner as SQL commands, except that a semicolon (;)
 * or a blank line does not terminate and execute a block.
 * Terminate PL/SQL subprograms by entering a period (.) by itself on a new line.
 * You can also terminate and execute a PL/SQL subprogram by entering a slash (/) by itself on a new line.
 * <p>
 * You enter the mode for entering PL/SQL statements when:
 * * You type DECLARE or BEGIN. After you enter PL/SQL mode in this way, type the remainder of your PL/SQL subprogram.
 * * You type a SQL command (such as CREATE PROCEDURE) that creates a stored procedure.
 * SQL*Plus stores the subprograms you enter the SQL buffer. Execute the current subprogram with a RUN or slash (/) command.
 * A semicolon (;) is treated as part of the PL/SQL subprogram and will not execute the command.
 */
final public class SqlLexer {


  private final SqlPlusLexerBuilder builder;

  private SqlLexer(SqlPlusLexerBuilder builder) {

    this.builder = builder;
  }

  public static SqlPlusLexerBuilder builder() {
    return new SqlPlusLexerBuilder();
  }

  public static List<SqlStatement> parseFromInputStream(InputStream inputStream) {
    return SqlLexer.builder().build().parse(inputStream);
  }


  public static List<SqlStatement> parseFromString(String script) {
    return SqlLexer.builder().build().parse(script);
  }

  public static List<SqlStatement> parseFromPath(Path path) {
    return builder().build().parse(path, null);
  }


  public List<SqlStatement> parse(Path path, Charset charset) {
    FsTextFile.FsTextFileBuilder fsTextFileBuilder = FsTextFile.builder(path).setCharset(charset);
    FsTextFile fileText;
    try {
      fileText = fsTextFileBuilder.build();
    } catch (FsTextDetectedCharsetNotSupported e) {
      throw new IllegalStateException("The character set detected (" + fsTextFileBuilder.getCharsetName() + ") is not supported by your OS. Path: " + path + ". Error: " + e.getMessage(), e);
    } catch (FsTextCharacterSetNotDetected e) {
      throw new IllegalArgumentException("We could not detect a character set for the path (" + path + "). 2 possibilities: it's not a text file or you should set a character set", e);
    }

    try (BufferedReader reader = fileText.getBufferedReader()) {
      return parse(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private List<SqlStatement> parse(BufferedReader reader) {
    SqlLexerTokenizer tokenizer = new SqlLexerTokenizer(builder, reader);
    List<SqlStatement> sqlStatements = new ArrayList<>();
    while (tokenizer.next()) {
      sqlStatements.add(tokenizer.getToken());
    }
    return sqlStatements;
  }

  public List<SqlStatement> parse(InputStream inputStream) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return parse(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<SqlStatement> parse(String string) {

    try (BufferedReader reader = new BufferedReader(new StringReader(string))) {
      return parse(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }


  enum ScanMode {

    /**
     * Sql
     * No empty line
     */
    SQL_MODE,

    /**
     * PlSql Statement
     * Empty Line are allowed
     */
    PSQL_MODE,

    /**
     * Command mode
     * (one command, one line)
     */
    COMMAND_MODE,

    /**
     * Comment mode
     */
    COMMENT_MODE,

    /**
     * Default mode to start the scan
     */
    START_MODE,


  }


  @SuppressWarnings("unused")
  public static class SqlPlusLexerBuilder {


    private String eol = System.lineSeparator();
    private InputStream inputStream;
    private String string;
    private FsTextFile fileText;
    // The characters that end a sql statement on a new Line
    private Set<String> newLineEndCommandCharacterSet = new HashSet<>(Arrays.asList(";", "/", ""));
    // The characters that end a plsql statement on a new Line
    private Set<String> newLineEndCommandPSqlCharacterSet = new HashSet<>(Arrays.asList(".", "/", "$$;"));
    // The characters that start a one-line comment
    // -- is the standard
    // # is mysql (https://dev.mysql.com/doc/refman/8.4/en/comments.html)
    private Set<String> startComments = new HashSet<>(List.of("--", "#"));
    // The characters that end a sql statement at the end of the line
    // The `;` is oracle,
    private Set<String> endOfLineEndCommandCharacterSet = new HashSet<>(List.of(";"));
    // The characters that end a pl sql statement at the end of a line
    // the // comes from example in MySQL set by the DELIMITER instruction
    // $$; is postgres
    private Set<String> endOfLineEndCommandPsqlCharacterSet = new HashSet<>(List.of("//", "$$;"));

    public Set<String> getEndOfLineEndCommandPsqlCharacterSet() {
      return endOfLineEndCommandPsqlCharacterSet;
    }


    public SqlPlusLexerBuilder setEndOfLineEndCommandCharacterSet(Set<String> endOfLineEndCommandCharacterSet) {
      this.endOfLineEndCommandCharacterSet = endOfLineEndCommandCharacterSet;
      return this;
    }


    public void setStartComments(Set<String> startComments) {
      this.startComments = startComments;
    }

    public SqlPlusLexerBuilder setNewLineEndCommandPSqlCharacterSet(Set<String> newLineEndCommandPSqlCharacterSet) {
      this.newLineEndCommandPSqlCharacterSet = newLineEndCommandPSqlCharacterSet;
      return this;
    }

    public SqlPlusLexerBuilder setNewLineEndCommandCharacterSet(Set<String> newLineEndCommandCharacterSet) {
      this.newLineEndCommandCharacterSet = newLineEndCommandCharacterSet;
      return this;
    }


    public SqlPlusLexerBuilder setEOL(String string) {
      this.eol = string;
      return this;
    }

    public SqlLexer build() {

      return new SqlLexer(this);

    }


    public Set<String> getNewLineEndCommandCharacterSet() {
      return this.newLineEndCommandCharacterSet;
    }

    public Set<String> getNewLineEndCommandPSqlCharacterSet() {
      return this.newLineEndCommandPSqlCharacterSet;
    }

    public Set<String> getCommentStrings() {
      return this.startComments;
    }

    public String getEol() {
      return this.eol;
    }

    public Set<String> getEndOfLineEndCommandCharacterSet() {
      return this.endOfLineEndCommandCharacterSet;
    }
  }
}
