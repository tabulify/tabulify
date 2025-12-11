package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

public class ArrayssTest {

    @Test
    public void arraysOfCharTest() {
        char[] endOfLineCharacters = {(char) 10, (char) 13};
        char c = "\n".charAt(0);
        boolean yesNo = Arrayss.in(endOfLineCharacters, c);
        Assert.assertEquals("The char was found",true,yesNo);

        yesNo = Arrayss.in(endOfLineCharacters, " ".charAt(0));
        Assert.assertEquals("The char was not found",false,yesNo);
    }

  @Test
  public void concatAddFirstElementTest() {

    String[] strings = {"Blue", "Balloon"};
    String firstElement = "A beautiful";
    String[] newStrings = Arrayss.concat(firstElement, strings);
    String[] expected = {firstElement, "Blue", "Balloon"};

    Assert.assertArrayEquals("The arrays are equal",expected,newStrings);

  }

  @Test
  public void concatTwoArraysTest() {

    String[] strings = {"Blue", "Balloon"};
    String[] firstElement = {"A beautiful","sun"};
    String[] newStrings = Arrayss.concat(firstElement, strings);
    String[] expected = {"A beautiful", "sun", "Blue", "Balloon"};

    Assert.assertArrayEquals("The arrays are equal",expected,newStrings);

  }


}
