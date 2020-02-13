package net.bytle.db.model;

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
 * <p>
 * See also:
 * * https://developers.google.com/public-data/docs/schema/dspl18
 * * https://html.spec.whatwg.org/#attr-input-typ - Html Forms Attributes Type
 * * https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html - Elastic Search
 * * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1 - Json schema
 * * https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#metadata-format - See data type description
 */
public class SqlDataType {

  public static Set<Integer> numericTypes = new HashSet<>();
  public static Set<Integer> characterTypes = new HashSet<>();
  public static Set<Integer> timeTypes = new HashSet<>();


  static {

    numericTypes.add(Types.NUMERIC); // numeric (10,0) - BigDecimal
    numericTypes.add(Types.INTEGER);
    numericTypes.add(Types.SMALLINT);
    numericTypes.add(Types.DOUBLE); // float
    numericTypes.add(Types.FLOAT);
    numericTypes.add(Types.DECIMAL);

    characterTypes.add(Types.VARCHAR);
    characterTypes.add(Types.CHAR);
    characterTypes.add(Types.NVARCHAR);
    characterTypes.add(Types.NCHAR);
    characterTypes.add(Types.CLOB);

    timeTypes.add(Types.DATE);
    timeTypes.add(Types.TIMESTAMP);

  }

  private Integer typeCode;
  // Multiple strings (because for instance VARCHAR is called TEXT in Sqlite)
  // The sqlite driver is also giving TEXT to the VARCHAR sql type
  // but you may create a column with the VARCHAR keyword ...
  private List<String> typeNames = new ArrayList<>();
  private Integer maxPrecision; // maximum precision
  private String literalPrefix; // prefix used to quote a literal (may be null)
  private String literalSuffix; // suffix used to quote a literal (may be null)
  private String createParams; // parameters used in creating the type (may be null)
  private Short nullable; // can you use null for this type
  private Boolean caseSensitive; // is it case sensitive
  private Short searchable; // can you use "WHERE" based on this type:
  private Boolean unsignedAttribute; //  is it unsigned
  private Boolean fixedPrecScale; // can it be a money value.
  private Boolean autoIncrement; // can it be used for an auto-increment value.
  private String localTypeName; // localized version of type name (may be null)
  private Integer minimumScale; // minimum scale supported
  private Integer maximumScale; // maximum scale supported
  private Class<?> clazz; // The java class that can hold this data type
  private String description;

  public SqlDataType(int typeCode) {
    this.typeCode = typeCode;
  }

  public static SqlDataType of(int typeCode) {
    return new SqlDataType(typeCode);
  }


  /**
   * The Java class that corresponds to the SQL Types.
   *
   * @return the java class that may contains this data
   */
  public Class<?> getClazz(){
    return this.clazz;
  }


  /**
   * @return the sql create statement part
   */
  public String getCreateStatement(int precision, int scale) {
    return null;
  }

  /**
   * @return the vendor class data type implementation
   */
  Class<?> getVendorClass() {
    return null;
  }


  public int getTypeCode(){
    return typeCode;
  }

  public List<String> getTypeNames(){
    return typeNames;
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
    if (maxPrecision==null){
      return Integer.MAX_VALUE;
    } else {
      return maxPrecision;
    }
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
   *  * typeNoNulls - does not allow NULL values
   *  * typeNullable - allows NULL values
   *  * typeNullableUnknown - nullability unknown
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
   *  * typePredNone - No support
   *  * typePredChar - Only supported with WHERE .. LIKE
   *  * typePredBasic - Supported except for WHERE .. LIKE
   *  * typeSearchable - Supported for all WHERE ..
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
   * @return can it be a money value.
   */
  public Boolean getFixedPrecScale() {
    return fixedPrecScale;
  }

  /**
   * @return can it be used for an auto-increment value.
   */
  public Boolean getAutoIncrement() {
    return autoIncrement;
  }

  /**
   * @return localized version of type name (may be null)
   */
  public String getLocalTypeName() {
    return localTypeName;
  }

  /**
   * @return minimum scale supported
   */
  public Integer getMinimumScale() {
    return minimumScale;
  }

  /**
   * @return maximum scale supported
   */
  public Integer getMaximumScale() {
    return maximumScale == null ? null : Integer.valueOf(maximumScale);
  }


  @Override
  public String toString() {
    return "DataType{" +
      "typeName='" + getTypeNames() + '\'' +
      ", typeCode=" + getTypeCode() +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SqlDataType that = (SqlDataType) o;
    return typeCode.equals(that.typeCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeCode);
  }

  public SqlDataType setTypeName(String typeName) {
    if (!this.typeNames.contains(typeName.toUpperCase())) {
      this.typeNames.add(typeName.toUpperCase());
    }
    return this;
  }

  public SqlDataType setMaxPrecision(int maxPrecision) {
    if (maxPrecision!=0) {
      this.maxPrecision = maxPrecision;
    }
    return this;
  }

  public SqlDataType setLiteralPrefix(String literalPrefix){
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

  public SqlDataType setUnsignedAttribute(Boolean unsignedAttribute){
    this.unsignedAttribute = unsignedAttribute;
    return this;
  }

  public SqlDataType setFixedPrecScale(Boolean fixedPrecScale) {
    this.fixedPrecScale = fixedPrecScale;
    return this;
  }

  public SqlDataType setAutoIncrement(Boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
    return this;
  }

  public SqlDataType setLocalTypeName(String localTypeName){
    this.localTypeName = localTypeName;
    return this;
  }

  public SqlDataType setMinimumScale(Integer minimumScale) {
    this.minimumScale = minimumScale;
    return this;
  }

  public SqlDataType setMaximumScale(Integer maximumScale) {
    this.minimumScale = minimumScale;
    return this;
  }

  public SqlDataType setClazz(Class<?> clazz) {
    this.clazz = clazz;
    return this;
  }

  public SqlDataType setDescription(String description) {
    this.description = description;
    return this;
  }
}
