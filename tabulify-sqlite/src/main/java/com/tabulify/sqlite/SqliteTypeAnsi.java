package com.tabulify.sqlite;

import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.model.SqlDataTypeVendor;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.util.List;

import static com.tabulify.sqlite.SqliteDataSystem.MAX_PRECISION_OR_SCALE;

/**
 * Ansi Class
 * Note as a child/aliases because we want to know the type
 * For instance, if json, not a alias of text because we want to know that the data is json
 * We rely on the type name of sqlite to detect the type
 */
public enum SqliteTypeAnsi implements SqlDataTypeVendor {

  DOUBLE_PRECISION(
    SqlDataTypeAnsi.DOUBLE_PRECISION,
    SqlDataTypeAnsi.DOUBLE_PRECISION.getAliases(),
    0
    ,
    0
  ),
  FLOAT(
    SqlDataTypeAnsi.FLOAT,
    SqlDataTypeAnsi.FLOAT.getAliases(),
    0,
    0
  ),
  DECIMAL(
    SqlDataTypeAnsi.DECIMAL,
    SqlDataTypeAnsi.DECIMAL.getAliases(),
    MAX_PRECISION_OR_SCALE,
    MAX_PRECISION_OR_SCALE
  ),
  /**
   * Boolean is supported as numeric 1
   */
  BOOLEAN(
    SqlDataTypeAnsi.BOOLEAN,
    SqlDataTypeAnsi.BOOLEAN.getAliases(),
    0,
    0
  ),
  MEDIUMINT(
    SqlDataTypeAnsi.MEDIUMINT,
    SqlDataTypeAnsi.MEDIUMINT.getAliases(),
    SqlDataTypeAnsi.MEDIUMINT.getMaxPrecision(),
    0
  ),
  BIGINT(
    SqlDataTypeAnsi.BIGINT,
    SqlDataTypeAnsi.BIGINT.getAliases(),
    SqlDataTypeAnsi.BIGINT.getMaxPrecision(),
    0
  ),
  SMALLINT(
    SqlDataTypeAnsi.SMALLINT,
    SqlDataTypeAnsi.SMALLINT.getAliases(),
    SqlDataTypeAnsi.SMALLINT.getMaxPrecision(),
    0
  ),
  TINYINT(
    SqlDataTypeAnsi.TINYINT,
    SqlDataTypeAnsi.TINYINT.getAliases(),
    SqlDataTypeAnsi.TINYINT.getMaxPrecision(),
    0
  ),

  /**
   * Note as a child of text because we want to know that it's a json data type
   * so the type name used should be json
   */
  JSON(
    SqlDataTypeAnsi.JSON,
    null,
    0,
    0
  ),
  /**
   * Note as a child of text because we want to know that it's a json data type
   * so the name used should be xml
   * sqlxml class is not supported
   */
  XML(
    SqlDataTypeAnsi.XML,
    null,
    0,
    0
  ),
  DATE(
    SqlDataTypeAnsi.DATE,
    null,
    0,
    0
  ),
  TIME(
    SqlDataTypeAnsi.TIME,
    null,
    0,
    0
  ),
  TIMESTAMP(
    SqlDataTypeAnsi.TIMESTAMP,
    SqlDataTypeAnsi.TIMESTAMP.getAliases(),
    // precision does not matter but we check that the precision is not null in our test
    7,
    0
  ),
  TIME_WITH_TIME_ZONE(
    SqlDataTypeAnsi.TIME_WITH_TIME_ZONE,
    SqlDataTypeAnsi.TIME_WITH_TIME_ZONE.getAliases(),
    0,
    0
  ),
  TIMESTAMP_WITH_TIME_ZONE(
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE,
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE.getAliases(),
    0,
    0
  ),
  CHARACTER_VARYING(
    SqlDataTypeAnsi.CHARACTER_VARYING,
    SqlDataTypeAnsi.CHARACTER_VARYING.getAliases(),
    SqliteTypeParser.MAX_LENGTH,
    0
  ),
  CHARACTER(
    SqlDataTypeAnsi.CHARACTER,
    SqlDataTypeAnsi.CHARACTER.getAliases(),
    SqliteTypeParser.MAX_LENGTH,
    0
  ),
  NATIONAL_CHARACTER(
    SqlDataTypeAnsi.NATIONAL_CHARACTER,
    SqlDataTypeAnsi.NATIONAL_CHARACTER.getAliases(),
    SqliteTypeParser.MAX_LENGTH,
    0
  ),
  NATIONAL_CHARACTER_VARYING(
    SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING,
    SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING.getAliases(),
    SqliteTypeParser.MAX_LENGTH,
    0
  );


  private final List<KeyInterface> aliases;
  private final SqlDataTypeAnsi ansi;
  private final KeyNormalizer name;
  private final int maxPrecision;
  private final int maxScale;

  SqliteTypeAnsi(SqlDataTypeAnsi sqlDataTypeAnsi, List<KeyInterface> aliases, int maxPrecision, int maxScale) {
    this.ansi = sqlDataTypeAnsi;
    this.aliases = aliases == null ? List.of() : aliases;
    this.name = KeyNormalizer.createSafe(this.name());
    this.maxPrecision = maxPrecision;
    this.maxScale = maxScale;
  }

  @Override
  public Class<?> getValueClass() {
    if (ansi == SqlDataTypeAnsi.XML) {
      return String.class;
    }
    return ansi.getValueClass();
  }

  @Override
  public int getMaxPrecision() {
    return maxPrecision;
  }

  @Override
  public int getMaximumScale() {
    return maxScale;
  }

  @Override
  public SqlDataTypeAnsi getAnsiType() {
    return ansi;
  }

  @Override
  public String getDescription() {
    return ansi.getDescription();
  }

  @Override
  public List<KeyInterface> getAliases() {
    return aliases;
  }

  @Override
  public String getName() {
    return this.name.toString();
  }

  @Override
  public String getVendor() {
    return "sqlite/ansi";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return ansi.getVendorTypeNumber();
  }
}
