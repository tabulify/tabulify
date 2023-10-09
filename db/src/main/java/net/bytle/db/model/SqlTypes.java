package net.bytle.db.model;

import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

/**
 * Extra class to extend {@link java.sql.Types}
 *
 * See also: https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql/type/SqlTypeName.html
 */
public class SqlTypes {


  /**
   * The Json type code
   * The value 1111 comes from the postgresql jdbc driver
   * You may see the value {@link java.sql.Types#OTHER} 1111
   * We choose arbitrary to go in the 3000 numeric space and take 3111
   * for JSON
   */
  public static final int JSON = 3111;

  /**
   * All numeric types (integer, fixed point, floating point)
   */
  public static Set<Integer> numericTypes = new HashSet<>();
  /**
   * Only the integers data type
   */
  public static Set<Integer> numericIntegerTypes = new HashSet<>();
  /**
   * Only the fixed points data type
   */
  public static Set<Integer> numericFixedPointTypes = new HashSet<>();
  /**
   * Only the floating point type
   */
  public static Set<Integer> numericFloatingPointTypes = new HashSet<>();
  /**
   * All characters types
   */
  public static Set<Integer> characterTypes = new HashSet<>();
  public static Set<Integer> timeTypes = new HashSet<>();



  static {

    /**
     * Fixed point number
     */
    SqlTypes.numericFixedPointTypes.add(Types.NUMERIC); // numeric (10,0) - BigDecimal
    SqlTypes.numericFixedPointTypes.add(Types.DECIMAL); //
    SqlTypes.numericTypes.addAll(SqlTypes.numericFixedPointTypes);
    /**
     * Integer
     */
    SqlTypes.numericIntegerTypes.add(Types.INTEGER);
    SqlTypes.numericIntegerTypes.add(Types.SMALLINT);
    SqlTypes.numericIntegerTypes.add(Types.BIGINT);
    SqlTypes.numericTypes.addAll(SqlTypes.numericIntegerTypes);
    /**
     * Floating Point Number
     */
    SqlTypes.numericFloatingPointTypes.add(Types.FLOAT); // float single precision (32 bit)
    SqlTypes.numericFloatingPointTypes.add(Types.DOUBLE); // float double precision (64 bit)
    SqlTypes.numericTypes.addAll(SqlTypes.numericFloatingPointTypes);

    /**
     * SQL defines two primary character types:
     *    character varying(n)
     *    and character(n),
     * where n is a positive integer.
     */
    SqlTypes.characterTypes.add(Types.VARCHAR);
    SqlTypes.characterTypes.add(Types.CHAR);
    SqlTypes.characterTypes.add(Types.NVARCHAR);
    SqlTypes.characterTypes.add(Types.NCHAR);
    SqlTypes.characterTypes.add(Types.CLOB);

    /**
     * Time
     */
    SqlTypes.timeTypes.add(Types.DATE);
    SqlTypes.timeTypes.add(Types.TIME);
    SqlTypes.timeTypes.add(Types.TIME_WITH_TIMEZONE);
    SqlTypes.timeTypes.add(Types.TIMESTAMP);
    SqlTypes.timeTypes.add(Types.TIMESTAMP_WITH_TIMEZONE);

  }

}
