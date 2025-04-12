package com.tabulify.fs.sql;

import com.tabulify.connection.Connection;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import static com.tabulify.fs.sql.FsSqlParsingModeValue.TEXT;


public class FsSqlDataPath extends FsTextDataPath {


  public static final String FILE_EXTENSION = "sql";
  protected static Set<String> FS_FILE_EXTENSION_OR_MIME = new HashSet<>();


  static {
    FS_FILE_EXTENSION_OR_MIME.add("application/sql");
  }

  @SuppressWarnings("FieldCanBeLocal")
  private Connection targetConnection;

  public FsSqlDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path, MediaTypes.TEXT_SQL);

    this.addVariablesFromEnumAttributeClass(FsSqlDataPathAttribute.class);

    this.setColumnName("sql");

    this.setEndOfRecords(
      ";\r", ";\r\n", ";\n",
      "/\r", "/\r\n", "/\n",
      "\rgo\r", "\r\ngo\r\n", "\ngo\n",
      "\rGO\r", "\r\nGO\r\n", "\nGO\n"
    );
  }


  /**
   * @param connection the target connection to be able to get the statement separator
   * @return the path for chaining
   */
  public FsSqlDataPath setTargetConnection(Connection connection) {
    this.targetConnection = connection;
    return this;
  }


  public FsSqlDataPath setParsingMode(FsSqlParsingModeValue parsingMode) {
    try {
      this.getVariable(FsSqlDataPathAttribute.PARSING_MODE).setOriginalValue(parsingMode);
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: PARSING_MODE variable was not found. It should not happen");
    }
    return this;
  }

  @Override
  public SelectStream getSelectStream() {
    if (this.getParsingModeValue() == TEXT) {
      return super.getSelectStream();
    } else {
      return new FsSqlParserStream(this);
    }
  }

  private FsSqlParsingModeValue getParsingModeValue() {
    try {
      return (FsSqlParsingModeValue) this.getVariable(FsSqlDataPathAttribute.PARSING_MODE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new RuntimeException("Internal Error: PARSING_MODE variable was not found. It should not happen");
    }
  }

  @Override
  public RelationDef createRelationDef() {
    FsSqlParsingModeValue parsingModeValue = this.getParsingModeValue();
    switch (parsingModeValue) {
      case TEXT:
        relationDef = new RelationDefDefault(this)
          .addColumn(this.getColumnName(), Types.CLOB);
        return relationDef;
      case SQL:
        // type is the type of sql:  sql, plsql, comment,…
        relationDef = new RelationDefDefault(this)
          .addColumn("type")
          .addColumn(this.getColumnName(), Types.CLOB);
        return relationDef;
      default:
        throw new IllegalStateException("Internal error: The parsing mode (" + parsingModeValue + ") should be in the case statement");
    }
  }
}
