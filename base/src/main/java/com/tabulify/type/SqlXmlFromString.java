package com.tabulify.type;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * An implementation of SQLXML above a string
 * when the driver or database does not support it
 */
public class SqlXmlFromString implements SQLXML {


  private String s;

  public SqlXmlFromString(String s) {
    this.s = s;
  }

  public static SQLXML create(String s) {
    return new SqlXmlFromString(s);
  }

  @Override
  public void free() throws SQLException {

  }

  @Override
  public InputStream getBinaryStream() throws SQLException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public OutputStream setBinaryStream() throws SQLException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Reader getCharacterStream() throws SQLException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Writer setCharacterStream() throws SQLException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public String getString() throws SQLException {
    return this.s;
  }

  @Override
  public void setString(String value) throws SQLException {
    this.s = value;
  }

  @Override
  public <T extends Source> T getSource(Class<T> sourceClass) throws SQLException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public <T extends Result> T setResult(Class<T> resultClass) throws SQLException {
    throw new UnsupportedOperationException("Not supported");
  }
}
