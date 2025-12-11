package com.tabulify.fs.sql;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoVariableException;
import com.tabulify.type.MediaTypes;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static com.tabulify.fs.sql.FsSqlParsingModeValue.TEXT;


public class FsSqlDataPath extends FsTextDataPath {


  protected static Set<String> FS_FILE_EXTENSION_OR_MIME = new HashSet<>();


  static {
    FS_FILE_EXTENSION_OR_MIME.add("application/sql");
  }


  public FsSqlDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path, MediaTypes.TEXT_SQL);

    this.addVariablesFromEnumAttributeClass(FsSqlDataPathAttribute.class);

    this.setColumnName(FsSqlParserColumn.SQL.toKeyNormalizer().toSqlCase());

    this.setEndOfRecords(
      ";\r", ";\r\n", ";\n",
      "/\r", "/\r\n", "/\n",
      "\rgo\r", "\r\ngo\r\n", "\ngo\n",
      "\rGO\r", "\r\nGO\r\n", "\nGO\n"
    );
  }


  public FsSqlDataPath setParsingMode(FsSqlParsingModeValue parsingMode) {
    try {
      this.getAttribute(FsSqlDataPathAttribute.PARSING_MODE).setPlainValue(parsingMode);
    } catch (NoVariableException e) {
      throw new InternalException("PARSING_MODE variable was not found. It should not happen");
    }
    return this;
  }

  @Override
  public SelectStream getSelectStream() {
    if (this.getParsingModeValue() == TEXT) {
      return super.getSelectStream();
    }
    return new FsSqlParserStream(this);
  }

  private FsSqlParsingModeValue getParsingModeValue() {
    try {
      return (FsSqlParsingModeValue) this.getAttribute(FsSqlDataPathAttribute.PARSING_MODE).getValueOrDefault();
    } catch (NoVariableException e) {
      throw new InternalException("PARSING_MODE variable was not found. It should not happen");
    }
  }

  @Override
  public RelationDef createRelationDef() {
    FsSqlParsingModeValue parsingModeValue = this.getParsingModeValue();
    switch (parsingModeValue) {
      case TEXT:
        relationDef = new RelationDefDefault(this)
          .addColumn(this.getColumnName(), SqlDataTypeAnsi.CLOB);
        return relationDef;
      case SQL:
        relationDef = new RelationDefDefault(this)
          .addColumn(FsSqlParserColumn.NAME.toKeyNormalizer().toSqlCase(), SqlDataTypeAnsi.CHARACTER_VARYING, 30)
          .addColumn(FsSqlParserColumn.SUBSET.toKeyNormalizer().toSqlCase(), SqlDataTypeAnsi.CHARACTER_VARYING, 30)
          .addColumn(FsSqlParserColumn.CATEGORY.toKeyNormalizer().toSqlCase(), SqlDataTypeAnsi.CHARACTER_VARYING, 30)
          .addColumn(this.getColumnName(), SqlDataTypeAnsi.CLOB);
        return relationDef;
      default:
        throw new InternalException("The parsing mode (" + parsingModeValue + ") should be in the case statement");
    }
  }
}
