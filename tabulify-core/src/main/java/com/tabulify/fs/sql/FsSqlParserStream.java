package com.tabulify.fs.sql;

import com.tabulify.stream.SelectStreamAbs;

import java.util.List;

public class FsSqlParserStream extends SelectStreamAbs {

  private final List<SqlPlusToken> tokens;
  private int counter = 0;
  private SqlPlusToken token;

  public FsSqlParserStream(FsSqlDataPath fsSqlDataPath) {
    super(fsSqlDataPath);
    this.tokens = SqlPlusLexer.createFromPath(fsSqlDataPath.getAbsoluteNioPath()).getSqlPlusTokens();
  }

  @Override
  public FsSqlDataPath getDataPath() {
    return (FsSqlDataPath) super.getDataPath();
  }

  @Override
  public boolean next() {
    if (tokens.size()>counter){
      token = tokens.get(counter);
      counter++;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void close() {

  }

  @Override
  public long getRow() {
    return counter;
  }

  @Override
  public Object getObject(int columnIndex) {
    if (columnIndex==1){
      return token.getCategory();
    } else if (columnIndex==2){
      return token.getContent();
    } else {
      throw new IndexOutOfBoundsException("There is no column with the index ("+columnIndex+")");
    }

  }

  @Override
  public void beforeFirst() {
    counter=0;
  }


}
