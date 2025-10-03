package com.tabulify.model;

import com.tabulify.connection.Connection;
import net.bytle.type.KeyNormalizer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Objects;

/**
 * The identifier of a data type is tricky.
 * You would say that it's its name but no
 * it's its name and its type code returned from {@link DatabaseMetaData#getTypeInfo()}
 * For instance,
 * * For postgres, a boolean is a bit(1) and not a boolean(1)
 * * For oracle, for the NUMBER type, you get multiple type one for {@link Types#BIT}, {@link Types#TINYINT}, {@link Types#BIGINT}, {@link Types#NUMERIC} {@link Types#INTEGER} and finally {@link Types#SMALLINT}
 * * For oracle, for the DATE name 2 lines with {@link Types#DATE}, {@link Types#TIMESTAMP} because the oracle data is a datetime
 * * For SQLServer, timestamp is a binary, not a timestamp (It's a row version number)
 * <p></p>
 * The fact that the key is name + type code eliminates the problem when the name does not correspond to its type code.
 * One more proof that the type identifier is its name and its jdbc code, is that all
 * method returns them (For instance, {@link ResultSetMetaData#getColumnTypeName(int)} and {@link ResultSetMetaData#getColumnType(int)}
 */
public class SqlDataTypeKey implements SqlDataTypeKeyInterface, Comparable<SqlDataTypeKey> {

  /**
   * The name used in the SQL statement
   * <p>
   * For instance, with the type code {@link Types#REAL}, in postgres, the sql name would be `float4`
   * The name used in a SQL statement
   * May be a single word or multiple (TINYINT UNSIGNED)
   * Never null
   * Not final has it can be changed by the driver
   * The type name is used in SQL statement
   **/
  private final KeyNormalizer name;
  /**
   * The type code retrieved from the driver and set for insertion
   * This type code should not change as this is the type code
   * that expects the driver to make any cast.
   * For instance, Postgres expect the type code to be {@link Types#OTHER} for the `json` data type
   */
  private final int typeCode;
  private final SqlTypeKeyUniqueIdentifier sqlTypeKeyUniqueIdentifier;


  public SqlDataTypeKey(Connection connection, KeyNormalizer name, int typeCode) {
    Objects.requireNonNull(name, "sqlName cannot be null");
    this.name = name;

    /**
     * Type code can be 0 ie {@link SqlDataTypeAnsi#NULL
     */
    this.typeCode = typeCode;

    sqlTypeKeyUniqueIdentifier = connection.getDataSystem().getSqlTypeKeyUniqueIdentifier();
  }

  // Getters
  @Override
  public String getName() {
    return name.toString();
  }

  @Override
  public String getVendor() {
    return "Unknown";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return typeCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    SqlDataTypeKey that = (SqlDataTypeKey) obj;
    if (sqlTypeKeyUniqueIdentifier.equals(SqlTypeKeyUniqueIdentifier.NAME_AND_CODE)) {
      return typeCode == that.typeCode && Objects.equals(name, that.name);
    }
    return Objects.equals(name, that.name);

  }

  @Override
  public int hashCode() {
    if (sqlTypeKeyUniqueIdentifier.equals(SqlTypeKeyUniqueIdentifier.NAME_AND_CODE)) {
      return Objects.hash(name, typeCode);
    }
    return Objects.hash(name);

  }

  @Override
  public String toString() {
    return String.format("%s (%d)", name, typeCode);
  }


  @Override
  public int compareTo(SqlDataTypeKey o) {
    return this.getName().compareTo(o.getName());
  }

  @Override
  public String name() {
    return getName();
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return name;
  }

}

