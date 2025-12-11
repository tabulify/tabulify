package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;


public class SqlClobTest {

  @Test
  public void clob() {
    String s = "Yolo";
    Clob stringClob = SqlClob.createFromString(s);
    Assert.assertEquals("The string should be equal with a string", s, stringClob.toString());
    Clob clobClob = SqlClob.createFromClob(stringClob);
    Assert.assertEquals("The string should be equal with a clob", s, clobClob.toString());
  }

  /**
   * A test demo that shows how to write in a clob
   *
   * @throws SQLException
   * @throws IOException
   */
  @Test
  public void writeWithWriter() throws SQLException, IOException {
    Clob myClob = SqlClob.create();
    Writer clobWriter = myClob.setCharacterStream(0);
    int n = 0;
    int length = 10;
    while (n < length) {
      clobWriter.write("a");
      n++;
    }
    clobWriter.flush();
    Assert.assertEquals("The text should be equals", "aaaaaaaaaa", clobWriter.toString());
    Assert.assertEquals("The text should be equals", "aaaaaaaaaa", myClob.toString());
    clobWriter.close();
  }

  /**
   * A read demo
   * @throws SQLException
   * @throws IOException
   */
  @Test
  public void readWithCharacterStream() throws SQLException, IOException {
    String s = "yolo";
    Clob lobData = SqlClob.createFromString(s);
    Reader reader = lobData.getCharacterStream();
    StringBuilder stringBuilder = new StringBuilder();
    int c;
    while ((c = reader.read()) != -1) {
      stringBuilder.append((char) c);
    }
    reader.close();
    lobData.free();
    Assert.assertEquals("Text should be the same", s, stringBuilder.toString());
  }

}
