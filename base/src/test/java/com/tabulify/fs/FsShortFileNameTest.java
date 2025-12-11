package com.tabulify.fs;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

public class FsShortFileNameTest {

  /**
   * Sfn test
   * https://en.wikipedia.org/wiki/8.3_filename
   */
  @Test
  public void isSfnTest() {

    // C:\Users\GERARD~1\AppData\Local\Temp\3631360781111678296\6007803182637292240.sql

    String[] strings = { "GERARD~1", "TEXTFI~1.TXT", "TEXTF~10.TXT"};
    Arrays.stream(strings).forEach(sfn->Assert.assertTrue("The string ("+sfn+") is sfn",FsShortFileName.of(sfn).isShortFileName()));

    String[] expectedShortName = { "GERARD", "TEXTFI", "TEXTF"};
    IntStream.range(0,strings.length).forEach(
      i->Assert.assertEquals("The string ("+strings[i]+") has the good name ("+expectedShortName[i]+")",expectedShortName[i], FsShortFileName.of(strings[i]).getShortName())
    );

  }
}
