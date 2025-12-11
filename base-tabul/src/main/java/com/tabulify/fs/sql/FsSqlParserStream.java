package com.tabulify.fs.sql;

import com.tabulify.model.ColumnDef;
import com.tabulify.stream.SelectStreamAbs;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;

import java.util.List;

public class FsSqlParserStream extends SelectStreamAbs {

  private final List<SqlStatement> tokens;
  private int counter = 0;
  private SqlStatement token;

  public FsSqlParserStream(FsSqlDataPath fsSqlDataPath) {
    super(fsSqlDataPath);
    this.tokens = SqlLexer.parseFromPath(fsSqlDataPath.getAbsoluteNioPath());
  }

  @Override
  public FsSqlDataPath getDataPath() {
    return (FsSqlDataPath) super.getDataPath();
  }

  @Override
  public boolean next() {
    if (tokens.size() > counter) {
      token = tokens.get(counter);
      counter++;
      return true;
    } else {
      return false;
    }
  }

  private boolean isClosed = false;
  @Override
  public void close() {
    this.isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return this.isClosed;
  }

  @Override
  public long getRecordId() {
    return counter;
  }



  @Override
  public Object getObject(ColumnDef columnDef) {
    FsSqlParserColumn sqlParserColumn;
    try {
      sqlParserColumn = Casts.cast(columnDef.getColumnName(), FsSqlParserColumn.class);
    } catch (CastException e) {
      // the sql column is the only one that the user can set
      sqlParserColumn = FsSqlParserColumn.SQL;
    }
    switch (sqlParserColumn) {
      case NAME: {
        return token.getKind().name().toLowerCase();
      }
      case SUBSET: {
        return token.getKind().getSqlSubset().name().toLowerCase();
      }
      case CATEGORY: {
        return token.getKind().getSqlTokenCategory().name().toLowerCase();
      }
      case SQL: {
        return token.getStatement();
      }
      default:
        throw new InternalException("The SQL Parser column (" + sqlParserColumn + ") was forgotten in the switch statement");
    }
  }

  @Override
  public void beforeFirst() {
    counter = 0;
  }


}
