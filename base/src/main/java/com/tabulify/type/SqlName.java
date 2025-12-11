package com.tabulify.type;

import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;

import java.util.List;

/**
 *
 */
public class SqlName {

  private final SqlNameConfig config;
  private final String name;

  private SqlName(String sqlName, SqlNameConfig config) {
    this.config = config;
    this.name = sqlName;
  }

  public static SqlName create(String sqlName) {
    return new SqlName(sqlName, SqlNameConfig.create());
  }

  public static SqlName create(String sqlName, SqlNameConfig sqlNameConfig) {
    return new SqlName(sqlName, sqlNameConfig);
  }

  public String toValidSqlName() {
    try {
      return toSqlName(true);
    } catch (CastException e) {
      throw new InternalException("Should not throw as it's with replacement. " + e.getMessage(), e);
    }
  }

  /**
   * @return a valid sql name
   * @throws CastException if the first letter is a digit
   *                       Use {@link #toValidSqlName()} if you don't care about it
   */
  public String toSqlName() throws CastException {
    return toSqlName(false);
  }


  /**
   * @param withReplacement - if true no exception is thrown
   * @return a valid sql name
   * @throws CastException - throw if the name is not valid, and replacement is false
   *                       throw always when the key is null, does not have any letter or digit
   */
  private String toSqlName(Boolean withReplacement) throws CastException {


    String sqlName = this.name;
    /**
     * get parts of letter and digits
     * Replace non-conforming characters with underscores
     */
    List<String> parts = KeyNormalizer.create(sqlName).getNormalizedParts();

    StringBuilder sanitized = new StringBuilder();
    for (int j = 0; j < parts.size(); j++) {
      String part = parts.get(j);
      if (j == 0) {
        char firstChar = parts.get(0).charAt(0);
        if (!String.valueOf(firstChar).matches("[a-zA-Z]")) {
          if (!withReplacement) {
            throw new CastException("Name (" + sqlName + ") is not valid for sql as it should start with a Latin letter (a-z, A-Z), not the character (" + firstChar + ")");
          } else {
            sanitized.append(config.getFirstLetter());
          }
        }
      }
      for (int i = 0; i < part.length(); i++) {
        char c = part.charAt(i);
        /**
         * valid char
         * Note it should not happen because we went already through {@link KeyNormalizer}
         * but yeah
         */
        if (String.valueOf(c).matches("[a-zA-Z0-9_]")) {
          sanitized.append(c);
        } else {
          if (withReplacement) {
            if (sanitized.length() == 0) {
              continue;
            }
            sanitized.append(config.getReplacementCharacter());
          } else {
            throw new CastException("Name (" + sqlName + ") is not valid for sql as it should not contain the character (" + c + ") only Latin letter (a-z, A-Z) and digits (0-9)");
          }
        }
      }
      if (j != parts.size() - 1) {
        sanitized.append(config.getReplacementCharacter());
      }
    }


    return sanitized.toString();
  }


}
