package com.tabulify.oracle;

import java.util.regex.Pattern;

public class OracleSqlUtil {

  private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("TIMESTAMP\\(\\d+\\)");

  /**
   * Unfortunately, the type in the table column is with precision (ie "TIMESTAMP(3)" and not "TIMESTAMP")
   * The driver returns them this way and while searching the metadata, we can also see them here:
   * ```
   * select distinct DATA_TYPE from ALL_TAB_COLUMNS where DATA_TYPE like 'TIME%' order by DATA_TYPE;
   * ```
   * This function removes the precision (scale) and parentheses from Oracle timestamp data types.
   * <p>
   * Examples:
   * - "TIMESTAMP(3)" -> "TIMESTAMP"
   * - "TIMESTAMP(6) WITH TIME ZONE" -> "TIMESTAMP WITH TIME ZONE"
   * - "TIMESTAMP(9) WITH TIME ZONE" -> "TIMESTAMP WITH TIME ZONE"
   * - "VARCHAR2(100)" -> "VARCHAR2(100)" (unchanged, not a timestamp type)
   *
   * @param dataType the Oracle data type string
   * @return the normalized data type with timestamp precision removed
   */
  public static String normalizeTimestampType(String dataType) {
    if (dataType == null) {
      return null;
    }

    // Replace TIMESTAMP(n) with just TIMESTAMP
    return TIMESTAMP_PATTERN.matcher(dataType).replaceAll("TIMESTAMP");
  }

}
