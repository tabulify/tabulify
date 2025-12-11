package com.tabulify.type;

/**
 * A config object that is normally bound to the sql database
 */
public class SqlNameConfig {

  private String firstLetter = "a";
  private String replacementCharacter = "_";


  public static SqlNameConfig create() {
    return new SqlNameConfig();
  }

  /**
   * @param firstLetter the first letter added to the name if it begins with a number (default to a)
   */
  public void setFirstLetter(String firstLetter) {
    this.firstLetter = firstLetter;
  }

  /**
   * @param replacementCharacter - the character used to replace bad sql character when making the sql name compliant
   */
  public void setReplacementCharacter(String replacementCharacter) {
    this.replacementCharacter = replacementCharacter;
  }


  public String getFirstLetter() {
    return this.firstLetter;
  }

  public String getReplacementCharacter() {
    return this.replacementCharacter;
  }

}
