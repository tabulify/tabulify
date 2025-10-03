package com.tabulify.sqlite;

import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.model.SqlDataTypeCommon;
import com.tabulify.model.SqlDataTypeVendor;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.sql.Types;
import java.util.List;
import java.util.Objects;

import static com.tabulify.sqlite.SqliteDataSystem.MAX_PRECISION_OR_SCALE;

/**
 * DataType in Sqlite has not rigid typing.
 * * one column can have several values with different type
 * * no type code
 * * the type description of the `create` statement is saved as is.
 * * one column has just an affinity (ie a preference)
 * * type is by cell and not by columns
 * <p>
 * <p>
 * DataType in Sqlite are declared via affinity that maps to a class storage
 * https://www.sqlite.org/datatype3.html
 * <p>
 * ^ Affinity ^ stores all data using storage classes ^
 * | TEXT     | NULL, TEXT or BLOB
 * | NUMERIC  | NULL, TEXT, BLOB, REAL, INTEGER (ALL)
 * | INTEGER  | Same as NUMERIC (The diff is on the cast expression)
 * | REAL     | Same as NUMERIC except that it forces integer values into floating point representation
 * | BLOB     | BLOB
 * <p>
 * The type affinity of a column is the recommended type for data stored in that column.
 * the type is recommended, not required. It can still be another storage
 * Columns with an affinity will prefer to use the affinity storage
 * <p>
 * <p>
 * The data type string used in the create statement
 * will be preserved but Sqlite has only four storage class
 * <p>
 * Every table column has a type affinity
 * Every cell has also an affinity
 */
public enum SqliteTypeAffinity implements SqlDataTypeVendor {

  // The value is a NULL value.
  NULL(Types.NULL, SqlDataTypeAnsi.NULL, null, 0, 0),

  // The integer value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
  // INT, INTEGER, TINYINT, SMALLINT, MEDIUMINT, BIGINT, UNSIGNED BIG INT, INT2, INT8
  INTEGER(
    Types.INTEGER,
    SqlDataTypeAnsi.INTEGER,
    SqlDataTypeAnsi.INTEGER.getAliases(),
    SqlDataTypeAnsi.INTEGER.getMaxPrecision(), // the precision is zero in the driver
    0
  ),
  // The real value is a floating point value, stored as an 8-byte IEEE floating point number.
  // REAL, FLOA, or DOUB declaration are treated as REAL
  // REAL, DOUBLE, DOUBLE PRECISION, FLOAT
  // The driver returns {@link Types#REAL} (ie 7) but it's a double
  REAL(
    Types.REAL,
    SqlDataTypeAnsi.DOUBLE_PRECISION,
    null,
    0,
    0
  ),

  // CHAR, VARCHAR, CLOB or TEXT declaration are treated as TEXT
  // CHARACTER(20), VARCHAR(255), VARYING CHARACTER(255), NCHAR(55), NATIVE CHARACTER(70), NVARCHAR(100), TEXT, CLOB
  // TEXT stores all data using storage classes NULL, TEXT or BLOB
  // Text as no precision, it's much more a longvarchar
  TEXT(Types.VARCHAR, SqlDataTypeAnsi.LONG_CHARACTER_VARYING, null, 0, 0),

  // The Blob value is a blob of data, stored exactly as it was input.
  BLOB(Types.BLOB, SqlDataTypeAnsi.BLOB, null, 0, 0),

  // NUMERIC, DECIMAL(10,5), BOOLEAN
  // With the exception of DATE, DATETIME that we store as text
  // numeric is not in the returned type of the driver
  NUMERIC(Types.NUMERIC,
    SqlDataTypeAnsi.NUMERIC,
    // NUM is created when using a `create table as select * from` on a table with timestamp type for instance
    List.of(SqlDataTypeCommon.NUM),
    MAX_PRECISION_OR_SCALE,
    MAX_PRECISION_OR_SCALE
  ),
  ;


  private final int jdbcCode;
  private final SqlDataTypeAnsi ansi;
  private final KeyNormalizer name;
  private final List<KeyInterface> aliases;
  private final int maximumPrecision;
  private final int maximumScale;

  SqliteTypeAffinity(int jdbcCode, SqlDataTypeAnsi sqlDataTypeAnsi, List<KeyInterface> aliases, int maximumPrecision, int maximumScale) {
    this.jdbcCode = jdbcCode;
    this.ansi = sqlDataTypeAnsi;
    this.name = KeyNormalizer.createSafe(this.name());
    this.aliases = Objects.requireNonNullElseGet(aliases, List::of);
    this.maximumPrecision = maximumPrecision;
    this.maximumScale = maximumScale;
  }

  @Override
  public Class<?> getValueClass() {
    return ansi.getValueClass();
  }

  @Override
  public int getMaxPrecision() {
    return maximumPrecision;
  }

  @Override
  public int getMaximumScale() {
    return maximumScale;
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
    return name.toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "sqlite/affinity";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return jdbcCode;
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return SqlDataTypeVendor.super.toKeyNormalizer();
  }

}
