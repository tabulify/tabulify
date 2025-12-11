package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;

public class StringsTest {


  @Test
  public void lTrim() {
    String s = "   Hallo   ";
    String lt = Strings.createFromString(s).ltrim().toString();
    Assert.assertEquals("The strings are equals", "Hallo   ", lt);
  }

  @Test
  public void rTrim() {
    String s = "Hallo   ";
    String rtEol = Strings.createFromString(s).rtrim().toString();
    Assert.assertEquals("The strings are equals", "Hallo", rtEol);
  }

  @Test
  public void rTrimEol() {
    String s = "Hallo \n\r\n";
    String rtEol = Strings.createFromString(s).rtrimEol().toString();
    Assert.assertEquals("The strings are equals", "Hallo ", rtEol);
  }

  @Test
  public void rTrimSuffix() {
    String s = "hallo.csv";
    String rtSuffix = Strings.createFromString(s).rtrim(".csv").toString();
    Assert.assertEquals("The strings are equals", "hallo", rtSuffix);
  }

  @Test
  public void toCamelCase() {
    String processString = Strings.createFromString("hallo").toFirstLetterCapitalCase().toString();
    Assert.assertEquals("The strings are equals", "Hallo", processString);
  }

  @Test
  public void trimStringCase() {
    String processString = Strings.createFromString("'hallo'").trim("'").toString();
    Assert.assertEquals("The strings are equals", "hallo", processString);
  }

  @Test
  public void fromPath() {
    int lineCount = Strings.createFromPath(Paths.get("./src/test/resources/type/Strings.txt")).getLineCount();
    Assert.assertEquals(6, lineCount);
  }

  @Test
  public void endOfLine() {
    String eol = "\r\n";
    String string = Strings.createFromString("Yolo" + eol + eol + "  ").getEol();
    Assert.assertEquals(eol, string);
    eol = "\r";
    string = Strings.createFromString("Yolo" + eol + eol + "  ").getEol();
    Assert.assertEquals(eol, string);
    eol = "\n";
    string = Strings.createFromString("Yolo" + eol + eol + "  ").getEol();
    Assert.assertEquals(eol, string);

  }

  @Test
  public void split() {
    List<String> names = Strings.createFromString("foo..").split(".");
    Assert.assertEquals(3, names.size());
  }
}
