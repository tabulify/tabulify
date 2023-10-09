package net.bytle.type;

import java.io.*;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * A clob:
 * * if the driver does not support one with {@link #create()} backed by a {@link StringWriter} that uses a ({@link StringBuffer}
 * * wrapper {@link #createFromClob(Clob)} to extract the string easily with {@link #toString()}
 */
public class SqlClob implements java.sql.Clob {


  private Clob clob = null;
  private StringWriter stringWriter = null;


  public SqlClob(Clob clob) {
    assert clob != null : "clob should not be null";
    this.clob = clob;
  }

  public SqlClob(StringWriter stringWriter) {
    this.stringWriter = stringWriter;
  }


  public static Clob create() {
    return new SqlClob(new StringWriter());
  }

  public static Clob createFromString(String string) {
    StringWriter stringWriter = new StringWriter();
    stringWriter.append(string);
    return new SqlClob(stringWriter);
  }

  public static Clob createFromClob(Clob clob) {
    return new SqlClob(clob);
  }

  public static Clob createFromObject(Object sourceObject) {
    if(sourceObject==null){
      return null;
    } else if (sourceObject instanceof String){
      return createFromString((String) sourceObject);
    } else if (sourceObject instanceof Clob){
      return createFromClob((Clob) sourceObject);
    } else {
     throw new IllegalArgumentException("The source Object ("+sourceObject.toString()+") has a class ("+sourceObject.getClass().getSimpleName()+") that cannot be transformed as a CLOB");
    }
  }

  public String toString() {

    if (clob != null) {
      try (
        Reader reader = clob.getCharacterStream();
        Writer writer = new StringWriter();
      ) {
        int c;
        while ((c = reader.read()) != -1) {
          writer.append((char) c);
        }
        clob.free();
        return writer.toString();

      } catch (IOException | SQLException e) {
        throw new RuntimeException(e);
      }
    } else {
      return this.stringWriter.toString();
    }
  }

  @Override
  public long length() throws SQLException {
    if (clob != null) {
      return clob.length();
    } else {
      return this.stringWriter.getBuffer().length();
    }
  }

  @Override
  public String getSubString(long pos, int length) throws SQLException {
    if (clob != null) {
      return clob.getSubString(pos, length);
    } else {
      int start = Long.valueOf(pos).intValue();
      return this.stringWriter.getBuffer().substring(start, start + length);
    }
  }

  @Override
  public Reader getCharacterStream() throws SQLException {
    if (clob != null) {
      return clob.getCharacterStream();
    } else {
      return new StringReader(this.stringWriter.toString());
    }
  }

  @Override
  public InputStream getAsciiStream() throws SQLException {
    if (clob != null) {
      return clob.getAsciiStream();
    } else {
      throw new UnsupportedOperationException("Not yet supported");
    }
  }

  @Override
  public long position(String searchstr, long start) throws SQLException {
    if (clob != null) {
      return clob.position(searchstr, start);
    } else {
      throw new UnsupportedOperationException("Not yet supported");
    }
  }

  @Override
  public long position(Clob searchstr, long start) throws SQLException {
    if (clob != null) {
      return clob.position(searchstr, start);
    } else {
      throw new UnsupportedOperationException("Not yet supported");
    }
  }

  @Override
  public int setString(long pos, String str) throws SQLException {
    if (clob != null) {
      return clob.setString(pos, str);
    } else {
      this.stringWriter.write(str, ((Long) pos).intValue(), str.length());
      return str.length();
    }
  }

  @Override
  public int setString(long pos, String str, int offset, int len) throws SQLException {
    if (clob!=null){
      return clob.setString(pos,str,offset,len);
    } else {
      throw new UnsupportedOperationException("Not yet supported");
    }
  }

  @Override
  public OutputStream setAsciiStream(long pos) throws SQLException {
    if (clob!=null){
      return clob.setAsciiStream(pos);
    } else {
      throw new UnsupportedOperationException("Not yet supported");
    }
  }

  /**
   * @param pos the character position where the stream start (1 indicates that the Writer object will start writing the stream of characters at the beginning of the Clob value)
   * @return
   * @throws SQLException
   */
  @Override
  public Writer setCharacterStream(long pos) throws SQLException {
    if (clob!=null){
      return clob.setCharacterStream(pos);
    } else {
      if (pos != 0 && pos != 1) {
        throw new IllegalStateException("The stream buffer position value should be 0 or 1");
      }
      return this.stringWriter;
    }
  }

  @Override
  public void truncate(long len) throws SQLException {
    if (clob!=null){
      clob.truncate(len);
    } else {
      this.stringWriter.getBuffer().delete(0,Long.valueOf(len).intValue());
    }
  }

  @Override
  public void free() throws SQLException {
    if (clob!=null){
      clob.free();
    } else {
      this.stringWriter = null;
    }
  }

  @Override
  public Reader getCharacterStream(long pos, long length) throws SQLException {
    if (clob!=null){
      return this.clob.getCharacterStream(pos,length);
    } else {
      return new StringReader(getSubString(pos, Math.toIntExact(length)));
    }
  }
}
