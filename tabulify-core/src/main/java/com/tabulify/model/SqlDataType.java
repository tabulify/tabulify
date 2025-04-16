package com.tabulify.model;

import com.tabulify.DbLoggers;
import com.tabulify.connection.Connection;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.*;

/**
 * The data type of a column.
 * <p>
 * DataType composition with the following order:
 * <p>
 * * dataTypeDatabase from the database definition
 * * dataTypeJdbc form the Jdbc Standard
 * <p>
 * <p>
 * A wrapper around {@link DatabaseMetaData#getTypeInfo()}
 * <p>
 * See also:
 * * https://developers.google.com/public-data/docs/schema/dspl18
 * * https://html.spec.whatwg.org/#attr-input-typ - Html Forms Attributes Type
 * * https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html - Elastic Search
 * * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1 - Json schema
 * * https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#metadata-format - See data type description
 */
public class SqlDataType {


  private final Connection connection;

  /**
   * The {@link Types}
   */
  private final Integer typeCode;

  /**
   * The driver type code is type code asks by the driver
   * By default, this is the same than {@link #typeCode}
   * but for type that are not in the {@link Types sql type}
   * such as {@link SqlTypes#JSON} for postgres, you need to
   * sent the {@link Types#OTHER} type code to the
   * {@link java.sql.PreparedStatement#setObject(int, Object, int)}
   */
  private Integer driverTypeCode;
  /**
   * The name used in the type code constant
   * <p>
   * With the type code {@link Types#REAL}, the name would be `real`
   * but it may be implemented via a {@link #getSqlName()} `float4` for instance
   */
  private String name;
  /**
   * The name used in a create statement
   * <p>
   * For instance, with the type code {@link Types#REAL}, in postgres, the sql name would be `float4`
   */
  private String sqlName;
  /**
   * Not sure what this is as name
   * Name could be translated in the local language ?
   */
  private String localTypeName; // localized version of type name (may be null)

  /**
   * The java class expected of the object
   * that is passed to the driver in the insert
   */
  private Class<?> sqlClazz;

  /**
   * Others properties
   */
  private Integer maxPrecision; // maximum precision
  private String literalPrefix; // prefix used to quote a literal (may be null)
  private String literalSuffix; // suffix used to quote a literal (may be null)
  private String createParams; // parameters used in creating the type (may be null)
  private Short nullable; // can you use null for this type
  private Boolean caseSensitive; // is it case sensitive
  private Short searchable; // can you use "WHERE" based on this type:
  private Boolean unsignedAttribute; //  is it unsigned
  private Boolean fixedPrecisionScale; // can it be a money value.
  private Boolean autoIncrement; // can it be used for an auto-increment value.
  private Integer minimumScale; // minimum scale supported
  private Integer maximumScale; // maximum scale supported
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private String description;
  private Integer defaultPrecision; // The default precision if not specified
  private Boolean mandatoryPrecision; // if the precision is mandatory in a clause statement


  public SqlDataType(Connection datastore, int typeCode) {

    this.typeCode = typeCode;
    switch (this.typeCode) {
      case Types.CHAR:
        this.name = "char";
        break;
      case Types.VARCHAR:
        this.name = "varchar";
        break;
      case Types.NCHAR:
        this.name = "nchar";
        break;
      case Types.NVARCHAR:
        this.name = "nvarchar";
        break;
      case Types.NUMERIC: // (fixed point number)
        this.name = "numeric";
        break;
      case Types.DECIMAL: // alias name for numeric (fixed point number)
        this.name = "decimal";
        break;
      case Types.INTEGER:
        this.name = "integer";
        break;
      case Types.REAL:
        this.name = "real";
        break;
      case Types.FLOAT:
        this.name = "float";
        break;
      case Types.DOUBLE:
        this.name = "double";
        break;
      case Types.DATE:
        this.name = "date";
        break;
      case Types.TIMESTAMP:
        this.name = "timestamp";
        break;
      case Types.TIMESTAMP_WITH_TIMEZONE:
        this.name = "timestamptz";
        break;
      case Types.TIME:
        this.name = "time";
        break;
      case Types.TIME_WITH_TIMEZONE:
        this.name = "timetz";
        break;
      case Types.BOOLEAN:
        this.name = "boolean";
        break;
      case SqlTypes.JSON:
        this.name = "json";
        break;
      case Types.SQLXML:
        this.name = "xml";
        break;
      case Types.CLOB:
        this.name = "clob";
        break;
      case Types.BLOB:
        this.name = "blob";
        break;
      case Types.BIGINT:
        this.name = "bigint";
        break;
      case Types.SMALLINT:
        this.name = "smallint";
        break;

    }
    this.connection = datastore;
  }

  public static SqlDataType creationOf(Connection datastore, int typeCode) {
    return new SqlDataType(datastore, typeCode);
  }


  /**
   * The Java class that is needed
   * to load the object with the JDBC driver
   *
   * @return the java class that may contain this data when loading a {@link java.sql.PreparedStatement#setObject(int, Object)}
   */
  public Class<?> getSqlClass() {

    if (this.sqlClazz == null) {
      DbLoggers.LOGGER_DB_ENGINE.warning("The class for the sql type (" + this + ") was null, we have returned a string");
      return String.class;
    }
    return this.sqlClazz;

  }


  public int getTypeCode() {
    return typeCode;
  }




  /**
   * The PRECISION column represents the maximum column size that the server supports for the given datatype.
   * For numeric data, this is the maximum precision.
   * For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component).
   * For binary data, this is the length in bytes.
   * For the ROWID datatype, this is the length in bytes.
   * Null is returned for data types where the column size is not applicable.
   */
  public Integer getMaxPrecision() {
    return maxPrecision;
  }

  /**
   * @return prefix used to quote a literal (may be null)
   */
  public String getLiteralPrefix() {
    return literalPrefix;
  }

  /**
   * @return suffix used to quote a literal (may be null)
   * TODO: Is This the thing before a word in select "myColumn", ...
   */
  public String getLiteralSuffix() {
    return literalSuffix;
  }

  /**
   * @return parameters used in creating the type (may be null)
   */
  public String getCreateParams() {
    return createParams;
  }

  /**
   * @return can you use null for this type
   * * typeNoNulls - does not allow NULL values
   * * typeNullable - allows NULL values
   * * typeNullableUnknown - nullability unknown
   */
  public Short getNullable() {
    return nullable;
  }

  /**
   * @return is it case sensitive
   */
  public Boolean getCaseSensitive() {
    return caseSensitive;
  }

  /**
   * @return can you use "WHERE" based on this type:
   * * typePredNone - No support
   * * typePredChar - Only supported with WHERE .. LIKE
   * * typePredBasic - Supported except for WHERE .. LIKE
   * * typeSearchable - Supported for all WHERE ..
   */
  public Short getSearchable() {
    return searchable;
  }

  /**
   * @return is it unsigned
   */
  public Boolean getUnsignedAttribute() {
    return unsignedAttribute;
  }

  /**
   * @return return if it can a money value.
   * Does not return if the declaration should have or not a
   */
  public Boolean isFixedPrecisionScale() {
    return fixedPrecisionScale;
  }

  /**
   * @return can it be used for an auto-increment value.
   */
  public Boolean getAutoIncrement() {
    return autoIncrement;
  }

  /**
   * @return localized version of type name (may be null) - in english, french ...
   */
  public String getLocalTypeName() {
    return localTypeName;
  }

  /**
   * @return minimum scale supported
   * See {@link DatabaseMetaData#getTypeInfo()}
   */
  public Integer getMinimumScale() {
    return minimumScale;
  }

  /**
   * @return maximum scale supported
   */
  public Integer getMaximumScale() {
    return maximumScale == null ? null : maximumScale;
  }


  @Override
  public String toString() {
    return getName() + " (" + getTypeCode() + "," + getSqlName() + ")@"+this.connection.getName();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SqlDataType that = (SqlDataType) o;
    return connection.equals(that.connection) &&
      typeCode.equals(that.typeCode) &&
      sqlName.equals(that.sqlName) &&
      Objects.equals(maxPrecision, that.maxPrecision) &&
      Objects.equals(literalPrefix, that.literalPrefix) &&
      Objects.equals(literalSuffix, that.literalSuffix) &&
      Objects.equals(createParams, that.createParams) &&
      Objects.equals(nullable, that.nullable) &&
      Objects.equals(caseSensitive, that.caseSensitive) &&
      Objects.equals(searchable, that.searchable) &&
      Objects.equals(unsignedAttribute, that.unsignedAttribute) &&
      Objects.equals(fixedPrecisionScale, that.fixedPrecisionScale) &&
      Objects.equals(autoIncrement, that.autoIncrement) &&
      Objects.equals(localTypeName, that.localTypeName) &&
      Objects.equals(minimumScale, that.minimumScale) &&
      Objects.equals(maximumScale, that.maximumScale) &&
      Objects.equals(sqlClazz, that.sqlClazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connection, typeCode, sqlName, maxPrecision, literalPrefix, literalSuffix, createParams, nullable, caseSensitive, searchable, unsignedAttribute, fixedPrecisionScale, autoIncrement, localTypeName, minimumScale, maximumScale, sqlClazz);
  }

  /**
   * The name used in a SQL statement
   *
   */
  public SqlDataType setSqlName(String typeName) {
    this.sqlName = typeName;
    return this;
  }

  public SqlDataType setMaxPrecision(Integer maxPrecision) {
    if (maxPrecision != null && maxPrecision != 0) {
      this.maxPrecision = maxPrecision;
    }
    return this;
  }

  public SqlDataType setLiteralPrefix(String literalPrefix) {
    this.literalPrefix = literalPrefix;
    return this;
  }

  public SqlDataType setLiteralSuffix(String literalSuffix) {
    this.literalSuffix = literalSuffix;
    return this;
  }

  public SqlDataType setCreateParams(String createParams) {
    this.createParams = createParams;
    return this;
  }

  public SqlDataType setNullable(Short nullable) {
    this.nullable = nullable;
    return this;
  }

  public SqlDataType setCaseSensitive(Boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public SqlDataType setSearchable(Short searchable) {
    this.searchable = searchable;
    return this;
  }

  public SqlDataType setUnsignedAttribute(Boolean unsignedAttribute) {
    this.unsignedAttribute = unsignedAttribute;
    return this;
  }

  public SqlDataType setFixedPrecisionScale(Boolean fixedPrecisionScale) {
    this.fixedPrecisionScale = fixedPrecisionScale;
    return this;
  }

  public SqlDataType setAutoIncrement(Boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
    return this;
  }

  public SqlDataType setLocalTypeName(String localTypeName) {
    this.localTypeName = localTypeName;
    return this;
  }

  public SqlDataType setMinimumScale(Integer minimumScale) {
    this.minimumScale = minimumScale;
    return this;
  }

  public SqlDataType setMaximumScale(Integer maximumScale) {
    this.maximumScale = maximumScale;
    return this;
  }

  /**
   * The java clazz that expects the driver to load the object in
   * this type
   *
   */
  public SqlDataType setSqlJavaClazz(Class<?> clazz) {
    if (clazz == null && this.connection.getTabular().isIdeEnv()) {
      throw new IllegalStateException("The class cannot be null for the type (" + this + ")");
    }
    this.sqlClazz = clazz;
    return this;
  }

  public SqlDataType setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * The sql name of the type
   * <p>
   * ie: if in {@link Types#REAL} it would have the {@link #getName()} real
   * but it may be implemented via a sql name `float4` for instance
   **/
  public String getSqlName() {
    // SQL Identifier are generally uppercase
    // but this is equivalent to shooting
    // It should be taken by a syntax highlighting
    return sqlName.toLowerCase();
  }


  public boolean isNumeric() {
    return SqlTypes.numericTypes.contains(this.getTypeCode());
  }

  /**
   * The name of the type
   * if in {@link Types#REAL} it would be real
   * but it may be implemented via a {@link #getSqlName()} `float4` for instance
   *
   */
  public String getName() {
    if (this.name != null) {
      return this.name;
    } else {
      return this.sqlName;
    }
  }

  public SqlDataType setName(String name) {
    this.name = name;
    return this;
  }


  public SqlDataType setDriverTypeCode(Integer driverTypeCode) {
    this.driverTypeCode = driverTypeCode;
    return this;
  }

  /**
   * @return the target type code for a {@link java.sql.PreparedStatement#setObject(int, Object, int)}
   * <p>
   * The {@link #driverTypeCode} if not null otherwise the {@link #typeCode}
   */
  public Integer getTargetTypeCode() {
    if (this.driverTypeCode != null) {
      return this.driverTypeCode;
    } else {
      return this.typeCode;
    }
  }

  public Integer getDefaultPrecision() {
    if (this.defaultPrecision == null) {
      return this.maxPrecision;
    } else {
      return this.defaultPrecision;
    }
  }

  /**
   * The precision that will be created when not given
   *
   */
  public SqlDataType setDefaultPrecision(Integer defaultPrecision) {
    this.defaultPrecision = defaultPrecision;
    return this;
  }


  /**
   * If the precision should be in the statement
   * Example nvarchar2 for oracle
   *
   */
  public SqlDataType setMandatoryPrecision(Boolean mandatoryPrecision) {
    this.mandatoryPrecision = mandatoryPrecision;
    return this;
  }

  public Boolean getMandatoryPrecision() {
    return this.mandatoryPrecision;
  }
}
