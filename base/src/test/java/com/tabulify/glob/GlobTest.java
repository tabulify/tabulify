package com.tabulify.glob;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GlobTest {

  @Test
  public void replaceWithMatchedWildCardsTest() {

    String string = "HalloWorld";
    Glob globPattern = Glob.createOf("Hallo*");

    String backReference = "Bonjour$1";
    String targetProcessed = globPattern.replace(string, backReference);
    Assert.assertEquals("The replacement has succeed", "BonjourWorld", targetProcessed);

    backReference = "$0";
    targetProcessed = globPattern.replace(string, backReference);
    Assert.assertEquals("The replacement of $0 has succeed", string, targetProcessed);

  }

  /**
   * Star should return always a match
   */
  @Test
  public void starTest() {

    Assertions.assertTrue(Glob.createOf("*").matches("yolo"));

  }


  @Test
  public void containsBackReferencesCharacters() {

    boolean contains = Glob.containsBackReferencesCharacters("Bonjour$1");
    Assert.assertTrue("The backreference was found", contains);

  }

  @Test
  public void getGroupsTest() {

    /**
     * One Star Capturing group
     */
    String source = "HalloWorld";
    Glob globPath = Glob.createOf("Hallo*");
    List<String> groups = globPath.getGroups(source);
    assertEquals("There is two groups", 2, groups.size());
    assertEquals("The first group is the good one", source, groups.get(0));
    assertEquals("The first capture is the good one", "World", groups.get(1));

    /**
     * Two Star Capturing group
     */
    globPath = Glob.createOf("H*W*");
    groups = globPath.getGroups(source);
    assertEquals("There is the good number of groups", 3, groups.size());
    assertEquals("The first group is the good one", source, groups.get(0));
    assertEquals("The first capture is the good one", "allo", groups.get(1));
    assertEquals("The second capture is the good one", "orld", groups.get(2));

    /**
     * Question Mark group
     */
    globPath = Glob.createOf("H?l?o*");
    groups = globPath.getGroups(source);
    assertEquals("Question Mark group - There is the good number of groups", 4, groups.size());
    assertEquals("Question Mark group - The first group is the good one", source, groups.get(0));
    assertEquals("Question Mark group - The first capture is the good one", "a", groups.get(1));
    assertEquals("Question Mark group - The first capture is the good one", "l", groups.get(2));
    assertEquals("Question Mark group - The second capture is the good one", "World", groups.get(3));

    /**
     * Class group
     */
    String testName = "Class group";
    globPath = Glob.createOf("[H]al?oWor*");
    groups = globPath.getGroups(source);
    assertEquals(testName + " - There is the good number of groups", 4, groups.size());
    assertEquals(testName + " - The first group is the good one", source, groups.get(0));
    assertEquals(testName + " - The first capture is the good one", "H", groups.get(1));
    assertEquals(testName + " - The first capture is the good one", "l", groups.get(2));
    assertEquals(testName + " - The second capture is the good one", "ld", groups.get(3));

    /**
     * Negation Class group
     * from https://en.wikipedia.org/wiki/Glob_(programming)
     */
    globPath = Glob.createOf("[!C]at");
    source = "Bat";
    groups = globPath.getGroups(source);
    testName = "Negation Class group";
    assertEquals(testName + " - There is the good number of groups", 2, groups.size());
    assertEquals(testName + " - The first group is the good one", source, groups.get(0));
    assertEquals(testName + " - The first capture is the good one", "B", groups.get(1));

    /**
     * Negation Class range group
     * from https://en.wikipedia.org/wiki/Glob_(programming)
     */
    globPath = Glob.createOf("Letter[!3-5]");
    source = "Letter1";
    groups = globPath.getGroups(source);
    testName = "Negation Class group Range";
    assertEquals(testName + " - There is the good number of groups", 2, groups.size());
    assertEquals(testName + " - The first group is the good one", source, groups.get(0));
    assertEquals(testName + " - The first capture is the good one", "1", groups.get(1));


  }

  @Test
  public void star_becomes_dot_star() {
    assertEquals(encloseStartEnd("gl.*b"), Glob.createOf("gl*b").toRegexPattern());
  }

  @Test
  public void escaped_star_is_unchanged() {
    assertEquals(encloseStartEnd("gl\\*b"), Glob.createOf("gl\\*b").toRegexPattern());
  }

  @Test
  public void question_mark_becomes_dot() {
    assertEquals(encloseStartEnd("gl.b"), Glob.createOf("gl?b").toRegexPattern());
  }

  @Test
  public void escaped_question_mark_is_unchanged() {
    assertEquals(encloseStartEnd("gl\\?b"), Glob.createOf("gl\\?b").toRegexPattern());
  }

  @Test
  public void character_classes_dont_need_conversion() {
    assertEquals(encloseStartEnd("gl[-o]b"), Glob.createOf("gl[-o]b").toRegexPattern());
  }

  @Test
  public void escaped_classes_are_unchanged() {
    assertEquals( encloseStartEnd("gl\\[-o\\]b"), Glob.createOf("gl\\[-o\\]b").toRegexPattern());
  }

  private String encloseStartEnd(String s) {
    return "^"+s+"$";
  }

  @Test
  public void negation_in_character_classes() {
    assertEquals(encloseStartEnd("gl[^a-n!p-z]b"), Glob.createOf("gl[!a-n!p-z]b").toRegexPattern());
  }

  @Test
  public void nested_negation_in_character_classes() {
    assertEquals(encloseStartEnd("gl[[^a-n]!p-z]b"), Glob.createOf("gl[[!a-n]!p-z]b").toRegexPattern());
  }

  @Test
  public void escape_carat_if_it_is_the_first_char_in_a_character_class() {
    assertEquals(encloseStartEnd("gl[\\^o]b"), Glob.createOf("gl[^o]b").toRegexPattern());
  }

  @Test
  public void metachars_are_escaped() {
    assertEquals(encloseStartEnd("gl..*\\.\\(\\)\\+\\|\\^\\$\\@\\%b"), Glob.createOf("gl?*.()+|^$@%b").toRegexPattern());
  }

  @Test
  public void metachars_in_character_classes_dont_need_escaping() {
    assertEquals(encloseStartEnd("gl[?*.()+|^$@%]b"), Glob.createOf("gl[?*.()+|^$@%]b").toRegexPattern());
  }

  @Test
  public void escaped_backslash_is_unchanged() {
    assertEquals(encloseStartEnd("gl\\\\b"), Glob.createOf("gl\\\\b").toRegexPattern());
  }

  @Test
  public void slashQ_and_slashE_are_escaped() {
    assertEquals(encloseStartEnd("\\\\Qglob\\\\E"), Glob.createOf("\\Qglob\\E").toRegexPattern());
  }

  @Test
  public void braces_are_turned_into_groups() {
    assertEquals(encloseStartEnd("(glob|regex)"), Glob.createOf("{glob,regex}").toRegexPattern());
  }

  @Test
  public void escaped_braces_are_unchanged() {
    assertEquals(encloseStartEnd("\\{glob\\}"), Glob.createOf("\\{glob\\}").toRegexPattern());
  }

  @Test
  public void commas_dont_need_escaping() {
    assertEquals(encloseStartEnd("(glob,regex),"), Glob.createOf("{glob\\,regex},").toRegexPattern());
  }


  @Test
  public void toSqlPatternTest() {
    String expected = "D\\_%";
    String sqlPattern = Glob.createOf("D_*").toSqlPattern("\\");
    Assert.assertEquals(expected, sqlPattern);
  }

  /**
   * When a Sql matchers (`_`, or `%`) is present in the
   * glob and that there is no escape character,
   * we should got an exception
   */
  @Test(expected = RuntimeException.class)
  public void toSqlPatternWithSqlMatcherTest() {
    Glob.createOf("D_*").toSqlPattern(null);
  }

}
