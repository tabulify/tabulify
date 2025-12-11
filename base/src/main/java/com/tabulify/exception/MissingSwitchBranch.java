package com.tabulify.exception;

/**
 * An exception to tell that a switch branch is missing
 */
public class MissingSwitchBranch extends InternalException {

  public MissingSwitchBranch(String variableName, Object value) {
    super(getText(variableName, value));
  }

  private static String getText(String variableName, Object value) {
    if (value == null) {
      return "A switch branch is missing for a value of the variable " + variableName;
    }
    return "A switch branch is missing for the value (" + value + ") of the variable " + variableName;
  }

}
