package net.bytle.exception;

import net.bytle.type.Enums;
import net.bytle.type.Key;

/**
 *
 * A utility class that creates {@link IllegalArgumentException}
 */

public class IllegalArgumentExceptions {

  public static IllegalArgumentException createForArgumentValue(String value, Enum<?> key, Class<?> enumClass, Throwable cause) {


    return createForArgumentValue(value, Key.toUriName(key), enumClass, cause);

  }

  public static IllegalArgumentException createForArgumentValue(String value, String key, Class<?> enumClass, Throwable cause) {

    String constants = Enums.toConstantAsStringOfUriAttributeCommaSeparated(enumClass);
    return new IllegalArgumentException("The value (" + value + ") is not a valid value for the option (" + key + "). You may choose one of the following values: " + constants, cause);

  }


  public static IllegalArgumentException createForStepArgument(String value, String step, Class<?> enumClass, Throwable cause) {

    String constants = Enums.toConstantAsStringOfUriAttributeCommaSeparated(enumClass);
    return new IllegalArgumentException("The value (" + value + ") is not a valid argument for the step (" + step + "). You may choose one of the following values: " + constants, cause);

  }


  public static IllegalArgumentException createFromValue(Object value, Throwable cause) {
    return new IllegalArgumentException("The value (" + value + ") is not a valid value", cause);
  }

  public static IllegalArgumentException createFromMessage(String message, Throwable e) {
    return new IllegalArgumentException(message, e);
  }
  public static IllegalArgumentException createFromMessage(String message) {
    return new IllegalArgumentException(message);
  }

  public static IllegalArgumentException createFromMessageWithPossibleValues(String message, Class<?> enumClass, Throwable e) {

    String constants = Enums.toConstantAsStringOfUriAttributeCommaSeparated(enumClass);

    return new IllegalArgumentException(message + " You may choose one of the following values: " + constants, e);
  }

  public static IllegalArgumentException createForArgumentValueForStep(String value, Enum<?> attribute, String step, Class<?> enumClass, Throwable cause) {

    return new IllegalArgumentException("The value (" + value + ") is not a valid value for the argument (" + attribute + ") on the step (" + step + ")." + Enums.toConstantAsStringCommaSeparated(enumClass), cause);
  }

  @SuppressWarnings("unused")
  public static IllegalArgumentException createForConnectionArgument(String value, String attribute, String connection, Class<?> enumClass, Throwable cause) {

    return IllegalArgumentExceptions.createFromMessageWithPossibleValues("The value (" + value + ") is not a valid value for the argument (" + attribute + ") on the connection (" + connection + ").", enumClass, cause);
  }

  public static IllegalArgumentException createFromException(Throwable e) {
    return new IllegalArgumentException(e);
  }
}
