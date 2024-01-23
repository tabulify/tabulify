package net.bytle.type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A key name normalizer
 */
public class KeyNameNormalizer {


  private final String stringOrigin;



  public enum WordCase {
    EVENT,
    HYPHEN,
    SNAKE,
    CAMEL,
    SQL,
    FILE
  }

  private final List<String> parts = new ArrayList<>();

  KeyNameNormalizer(String stringOrigin) {

    this.stringOrigin = stringOrigin;

    StringBuilder currentWord = new StringBuilder();
    for (char c : stringOrigin.toCharArray()) {
      // Separator
      if (Character.isWhitespace(c) || Character.isUpperCase(c)) {
        if (currentWord.length() > 0) {
          parts.add(currentWord.toString());
          currentWord.setLength(0);
        }
      }
      if (Character.isWhitespace(c)) {
        // we don't collect whitespace
        continue;
      }
      currentWord.append(c);
    }

    if (currentWord.length() > 0) {
      parts.add(currentWord.toString());
    }

  }

  public static KeyNameNormalizer createFromString(String name) {
    return new KeyNameNormalizer(name);
  }

  public String toCamelCase() {
    return this.parts.stream()
      .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
      .collect(Collectors.joining());
  }

  /**
   *
   * @return a name in event case. ie camel case with a space between words
   */
  public String toEventCase() {
    return this.parts.stream()
      .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
      .collect(Collectors.joining(" "));
  }

  /**
   * @return the words in a Snake Case (ie user_count)
   */
  public String toSnakeCase() {
    return this.parts.stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("_"));
  }

  /**
   * @return the words in a Upper Snake Case (ie USER_COUNT)
   */
  public String toUpperSnakeCase() {
    return this.parts.stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("_"));
  }

  /**
   * @return the words in a Upper Snake Case (ie USER_COUNT)
   */
  public String toSqlCase() {
    return this.toUpperSnakeCase();
  }

  public String toWordCase(WordCase wordCase) {
    switch (wordCase) {
      case EVENT:
        return toEventCase();
      case CAMEL:
        return toCamelCase();
      case HYPHEN:
        return toHyphenCase();
      case FILE:
        return toFileCase();
      case SNAKE:
        return toSnakeCase();
      case SQL:
        return toSqlCase();
      default:
        throw new IllegalArgumentException("The word-case (" + wordCase + ") is unknown");
    }
  }

  /**
   * @return the words in a Hypen Case (ie user-count)
   */
  public String toHyphenCase() {
    return this.parts.stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("-"));
  }

  /**
   *
   * @return a name that can be used as file name in the file system (ie the {@link #toHyphenCase()}
   */
  public String toFileCase() {
    return toHyphenCase();
  }

  @Override
  public String toString() {
    return this.stringOrigin;
  }


}
