package com.tabulify.model;

import net.bytle.type.KeyNormalizer;

import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

/**
 * Static Utility
 * Extra class to extend {@link Types}
 *
 * See also: <a href="https://calcite.apache.org/javadocAggregate/org/apache/calcite/sql/type/SqlTypeName.html">...</a>
 */
public class SqlDataTypes {


  /**
   * All numeric types (integer, fixed point, floating point)
   */
  public static Set<SqlDataTypeAnsi> numberTypes = new HashSet<>();
  /**
   * Only the integers data type
   */
  public static Set<SqlDataTypeAnsi> numberIntegerTypes = new HashSet<>();
  /**
   * Only the fixed points data type
   */
  public static Set<SqlDataTypeAnsi> numberFixedPointTypes = new HashSet<>();
  /**
   * Only the floating point type
   */
  public static Set<SqlDataTypeAnsi> numberFloatingPointTypes = new HashSet<>();
  /**
   * All characters types
   */
  public static Set<SqlDataTypeAnsi> characterTypesWithLength = new HashSet<>();
  public static Set<SqlDataTypeAnsi> timeTypes = new HashSet<>();

  static {

    /**
     * Fixed point number
     */
    numberFixedPointTypes.add(SqlDataTypeAnsi.NUMERIC); // numeric (10,0) - BigDecimal
    numberFixedPointTypes.add(SqlDataTypeAnsi.DECIMAL); //
    numberTypes.addAll(numberFixedPointTypes);
    /**
     * Integer
     */
    numberIntegerTypes.add(SqlDataTypeAnsi.INTEGER);
    numberIntegerTypes.add(SqlDataTypeAnsi.SMALLINT);
    numberIntegerTypes.add(SqlDataTypeAnsi.BIGINT);
    numberIntegerTypes.add(SqlDataTypeAnsi.TINYINT);
    numberTypes.addAll(numberIntegerTypes);

    /**
     * Floating Point Number
     */
    numberFloatingPointTypes.add(SqlDataTypeAnsi.REAL); // float single precision (32 bit)
    numberFloatingPointTypes.add(SqlDataTypeAnsi.FLOAT); // float variable precision from (32 bit to 64)
    numberFloatingPointTypes.add(SqlDataTypeAnsi.DOUBLE_PRECISION); // float double precision (64 bit)
    numberTypes.addAll(numberFloatingPointTypes);

    /**
     * Character types with length
     * SQL defines two primary character types:
     *    character varying(n)
     *    and character(n),
     * where n is a positive integer.
     */
    characterTypesWithLength.add(SqlDataTypeAnsi.CHARACTER_VARYING);
    characterTypesWithLength.add(SqlDataTypeAnsi.CHARACTER);
    characterTypesWithLength.add(SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING);
    characterTypesWithLength.add(SqlDataTypeAnsi.NATIONAL_CHARACTER);

    /**
     * Time
     */
    timeTypes.add(SqlDataTypeAnsi.DATE);
    timeTypes.add(SqlDataTypeAnsi.TIME);
    timeTypes.add(SqlDataTypeAnsi.TIME_WITH_TIME_ZONE);
    timeTypes.add(SqlDataTypeAnsi.TIMESTAMP);
    timeTypes.add(SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE);

  }


  /**
   * @return the sql name of the root type
   */
  public static KeyNormalizer getTopSqlName(SqlDataType targetSqlType) {
    return targetSqlType.getParentOrSelf().toKeyNormalizer();
  }
}
