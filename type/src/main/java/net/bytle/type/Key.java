package net.bytle.type;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Key utility function
 * @deprecated should be moved to {@link KeyNormalizer}
 */
public class Key {

  /**
   * Return a key normalized
   * <p>
   * (ie no space, no separator)
   * <p>
   * It allows entering key:
   * * with a separator
   * * or CamelCase
   * <p>
   * for a better reading experience in a config file or in the code
   * <p>
   * Usage:
   * <p>
   * toNormalizedKey(codeKey).equals(toNormalizedKey(confKey)
   *
   * @param publicKey - a public key
   * @return the normalized key
   */
  public static String toNormalizedKey(String publicKey) {
    return publicKey.toLowerCase()
      .trim()
      .replace("_", "")
      .replace("-", "")
      .replace(" ", "")
      ;
  }

  /**
   * @param string a string
   * @return a string that is SQL column name proof
   */
  public static String toColumnName(String string) {
    return string.toLowerCase()
      .replace(" ", "_")
      .replace("\\", "_")
      .replace("/", "_")
      .replace(".", "_")
      .replace("-", "_");
  }

  public static String toColumnName(Enum<?> enumValue) {
    return Key.toColumnName(enumValue.toString());
  }

  /**
   * @param string - an attribute name or domain value
   * @return the public facing value
   */
  public static String toCamelCaseValue(String string) {
    return splitName(string)
      .stream()
      .map(s -> Strings.createFromString(s).toFirstLetterCapitalCase().toString())
      .collect(Collectors.joining());
  }


  /**
   * @param enumValue the enum value
   * @return the long option name is a series of lowercase name separated by a minus (used by the command line and in HTML template variable)
   */
  public static String toLongOptionName(Enum<?> enumValue) {

    return toLongOptionName(enumValue.toString());
  }

  /**
   * @param string the enum value
   * @return the long option name is a series of lowercase name separated by a minus (used by the command line and in HTML template variable)
   */
  public static String toLongOptionName(String string) {

    return splitName(string)
      .stream()
      .map(String::toLowerCase)
      .collect(Collectors.joining("-"));
  }

  /**
   * @param enumValue the enum value
   * @return the short option name for the command line are the first lowercase letter of each name
   */
  public static String toShortOptionName(Enum<?> enumValue) {
    return toShortOptionName(enumValue.toString());
  }

  public static String toShortOptionName(String string) {

    return splitName(string)
      .stream()
      .map(s -> String.valueOf(s.charAt(0)).toLowerCase())
      .collect(Collectors.joining());

  }

  /**
   * @param string a string
   * @return the string splitted in names
   */
  public static List<String> splitName(String string) {


    String pattern;
    if (Strings.isUpperCase(string)) {
      pattern = "[-_]";
    } else {
      pattern = "[-_A-Z]";
    }
    return Strings.createFromString(string)
      .splitWithoutRemovingTheSplitCharacter(pattern)
      .stream()
      .map(e -> Strings.createFromString(e).trim("_").trim("-").toString())
      .filter(e -> !e.equals(""))
      .collect(Collectors.toList());


  }

  public static String toUriName(String name) {
    return toLongOptionName(name);
  }

  public static String toUriName(Enum<?> enumValue) {
    return toLongOptionName(enumValue);
  }

}
