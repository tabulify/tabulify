package com.tabulify.sqlite;


import com.tabulify.connection.Connection;
import com.tabulify.model.SqlDataType;
import net.bytle.log.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class utility used to extract type information from the PRAGMA table_info();
 * and return type information (typeName, Precision and scale)
 */
public class SqliteType {

  public static final int MAX_NUMERIC_PRECISION = Integer.MAX_VALUE;
  private static final Log LOGGER = Sqlites.LOGGER_SQLITE;
  private final Connection datastore;

  // https://www.sqlite.org/limits.html#max_length (2^31-1)
  static protected int MAX_LENGTH = 2147483647;

  String type;
  Integer scale;
  Integer precision;

  private SqliteType(Connection connection, String type, Integer precision, Integer scale) {
    this.type = type;
    this.scale = scale;
    this.precision = precision;
    this.datastore = connection;
  }

  /**
   * @param connection the connection
   * @param description - A datatype string definition in the form:
   *                    * type(precision, scale)
   *                    * type(precision)
   *                    * type
   * @return a data type
   * <p>
   * Example: INTEGER(50,2)
   */

  static public SqliteType create(Connection connection, String description) {
    Pattern pattern = Pattern.compile("\\s*([^(]+)\\s*(?:\\(([^)]+)\\))?\\s*");
    Matcher matcher = pattern.matcher(description);
    String typeName = null;
    Integer scale = null;
    Integer precision = null;
    while (matcher.find()) {

      typeName = matcher.group(1);
      String scaleAndPrecision = matcher.group(2);
      if (scaleAndPrecision != null) {
        String[] array = scaleAndPrecision.split(",");
        try {
          precision = Integer.valueOf(array[0]);
        } catch (Exception e){
          // It's possible to get a double format here because of the pragma
          precision = Double.valueOf(array[0]).intValue();
        }

        if (array.length == 2) {
          scale = Integer.valueOf(array[1]);
        }
      }

    }
    return new SqliteType(connection, typeName, precision, scale);
  }

  public String getTypeName() {
    return type;
  }

  public Integer getTypeCode() {
    final SqlDataType of = this.datastore.getSqlDataType(type);
    if (of == null) {
      LOGGER.warning("The type code is unknown for the type (" + type + ")");
      return null;
    } else {
      return of.getTypeCode();
    }
  }

  public Integer getScale() {
    return this.scale;
  }

  public Integer getPrecision() {
    return this.precision;
  }
}
