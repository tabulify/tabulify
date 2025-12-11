package com.tabulify.sqlite;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class utility used to extract type information from the PRAGMA table_info();
 * and return type information (typeName, Precision and scale)
 */
public class SqliteTypeParser {

  public static final int MAX_NUMERIC_PRECISION = Integer.MAX_VALUE;

  // https://www.sqlite.org/limits.html#max_length (2^31-1)
  static protected int MAX_LENGTH = 2147483647;

  String type;
  int scale = 0;
  int precision = 0;

  private SqliteTypeParser(String type, int precision, int scale) {
    this.type = type;
    this.scale = scale;
    this.precision = precision;
  }

  /**
   * @param description - A datatype string definition in the form:
   *                    * type(precision, scale)
   *                    * type(precision)
   *                    * type
   * @return a data type
   * <p>
   * Example: INTEGER(50,2)
   */

  static public SqliteTypeParser create(String description) {
    Pattern pattern = Pattern.compile("\\s*([^(]+)\\s*(?:\\(([^)]+)\\))?\\s*");
    Matcher matcher = pattern.matcher(description);
    String typeName = null;
    int scale = 0;
    int precision = 0;
    while (matcher.find()) {

      typeName = matcher.group(1);
      String scaleAndPrecision = matcher.group(2);
      if (scaleAndPrecision != null) {
        String[] array = scaleAndPrecision.split(",");
        try {
          precision = Integer.parseInt(array[0]);
        } catch (Exception e){
          // It's possible to get a double format here because of the pragma
          precision = Double.valueOf(array[0]).intValue();
        }

        if (array.length == 2) {
          scale = Integer.parseInt(array[1]);
        }
      }

    }
    return new SqliteTypeParser(typeName, precision, scale);
  }

  public String getTypeName() {
    return type;
  }


  public int getScale() {
    return this.scale;
  }

  public int getPrecision() {
    return this.precision;
  }
}
