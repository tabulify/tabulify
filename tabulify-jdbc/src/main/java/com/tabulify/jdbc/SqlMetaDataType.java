package com.tabulify.jdbc;

import java.sql.DatabaseMetaData;

/**
 * Represents the data type meta that we get from the database
 * This object is used to be able to path information
 * to {@link SqlDataStoreProvider}
 * in order to correct them
 * before creating the data type
 * <p>
 * By default, they are created with the {@link SqlDataSystem#getMetaDataTypes()}
 * that takes this information from the JDBC driver {@link DatabaseMetaData#getTypeInfo()}
 */
public class SqlMetaDataType {

  private final int typeCode;
  private String typeName;
  private Integer maxPrecision;
  private String literalPrefix;
  private String literalSuffix;
  private String createParams;
  private Short nullable;
  private Boolean caseSensitive;
  private Short searchable;
  private Boolean unsignedAttribute;
  private Boolean fixedPrecisionScale;
  private Boolean autoIncrement;
  private String localTypeName;
  private Integer minimumScale;
  private Integer maximumScale;
  private Class<?> clazz;
  private Integer driverTypeCode;
  private Integer defaultPrecision;
  private Boolean mandatoryPrecision;

  public SqlMetaDataType(int typeCode) {
    this.typeCode = typeCode;
  }

  public SqlMetaDataType setSqlName(String typeName) {
    this.typeName = typeName;
    return this;
  }

  public SqlMetaDataType setMaxPrecision(Integer maxPrecision) {
    this.maxPrecision = maxPrecision;
    return this;
  }

  public SqlMetaDataType setLiteralPrefix(String literalPrefix) {
    this.literalPrefix = literalPrefix;
    return this;
  }

  public SqlMetaDataType setLiteralSuffix(String literalSuffix) {
    this.literalSuffix = literalSuffix;
    return this;
  }

  public SqlMetaDataType setCreateParams(String createParams) {
    this.createParams = createParams;
    return this;
  }

  public SqlMetaDataType setNullable(Short nullable) {
    this.nullable = nullable;
    return this;
  }

  public SqlMetaDataType setCaseSensitive(Boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public SqlMetaDataType setSearchable(Short searchable) {
    this.searchable = searchable;
    return this;
  }

  public SqlMetaDataType setUnsignedAttribute(Boolean unsignedAttribute) {
    this.unsignedAttribute = unsignedAttribute;
    return this;
  }

  public SqlMetaDataType setFixedPrecisionScale(Boolean fixedPrecScale) {
    this.fixedPrecisionScale = fixedPrecScale;
    return this;
  }

  public SqlMetaDataType setAutoIncrement(Boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
    return this;
  }

  public SqlMetaDataType setLocalTypeName(String localTypeName) {
    this.localTypeName = localTypeName;
    return this;
  }

  public SqlMetaDataType setMinimumScale(Integer minimumScale) {
    this.minimumScale = minimumScale;
    return this;
  }

  public SqlMetaDataType setMaximumScale(Integer maximumScale) {
    this.maximumScale = maximumScale;
    return this;
  }

  public Integer getTypeCode() {
    return this.typeCode;
  }

  public Integer getMaxPrecision() {
    return this.maxPrecision;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public Integer getMaximumScale() {
    return this.maximumScale;
  }

  public Boolean getAutoIncrement() {
    return this.autoIncrement;
  }

  public Integer getMinimumScale() {
    return this.minimumScale;
  }

  public Boolean getCaseSensitive() {
    return this.caseSensitive;
  }

  public String getCreateParams() {
    return this.createParams;
  }

  public Boolean getFixedPrecisionScale() {
    return this.fixedPrecisionScale;
  }

  public String getLiteralPrefix() {
    return this.literalPrefix;
  }

  public String getLiteralSuffix() {
    return this.literalSuffix;
  }

  public Short getNullable() {
    return this.nullable;
  }

  public String getLocalTypeName() {
    return this.localTypeName;
  }

  public Short getSearchable() {
    return this.searchable;
  }

  public Boolean getUnsignedAttribute() {
    return this.unsignedAttribute;
  }

  public SqlMetaDataType setSqlJavaClazz(Class<?> clazz) {
    this.clazz = clazz;
    return this;
  }

  public Class getSqlClass() {
    return this.clazz;
  }

  @Override
  public String toString() {
    return typeName + "(" + typeCode + ')';
  }

  /**
   * The type code asked by the driver
   * This was created because PostGres for the JSON data type
   * require the {@link java.sql.Types#OTHER}
   * @param driverTypeCode
   * @return
   */
  public SqlMetaDataType setDriverTypeCode(int driverTypeCode) {
    this.driverTypeCode = driverTypeCode;
    return this;
  }

  public Integer getDriverTypeCode() {
    return this.driverTypeCode;
  }

  /**
   * The precision given when not set
   * @param defaultPrecision
   * @return
   */
  public SqlMetaDataType setDefaultPrecision(Integer defaultPrecision) {
    this.defaultPrecision = defaultPrecision;
    return this;
  }

  public Integer getDefaultPrecision() {
    return this.defaultPrecision;
  }

  public SqlMetaDataType setPrecisionMandatory(boolean b) {
    this.mandatoryPrecision = true;
    return this;
  }

  public Boolean getMandatoryPrecision() {
    return this.mandatoryPrecision;
  }
}
