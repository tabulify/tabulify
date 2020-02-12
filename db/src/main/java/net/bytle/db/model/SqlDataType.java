package net.bytle.db.model;

import net.bytle.db.database.DataTypeDatabase;

import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

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
public abstract class SqlDataType {

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




  private DataTypeDatabase dataTypeDatabase;


  public DataTypeDatabase getDataTypeDatabase() {
    return dataTypeDatabase;
  }



  /**
   * The Java class that corresponds to the SQL Types.
   *
   * @return
   */
  public Class<?> getClazz() {

    final DataTypeDatabase dataTypeDatabase = this.dataTypeDatabase;
    if (this.dataTypeDatabase != null) {
      if (dataTypeDatabase.getJavaDataType() != null) {
        return dataTypeDatabase.getJavaDataType();
      }
    }

    throw new RuntimeException("The data type" + getTypeName() + "(" + getTypeCode() + " has no class java defined");

  }


  public abstract int getTypeCode();

  public abstract String getTypeName();

  /**
   * The PRECISION column represents the maximum column size that the server supports for the given datatype.
   * For numeric data, this is the maximum precision.
   * For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component).
   * For binary data, this is the length in bytes.
   * For the ROWID datatype, this is the length in bytes.
   * 0 is returned for data types where the column size is not applicable or unknown
   */
  public Integer getMaxPrecision() {


    // A character must have always a precision
    // but not a number
    // TODO: This may cause a problem if the driver returns null as maxPrecision ...
    return 0;

  }

  /**
   * @return prefix used to quote a literal (may be null)
   */
  public String getLiteralPrefix() {

    return null;

  }

  /**
   * @return suffix used to quote a literal (may be null)
   * TODO: Is This the thing before a word in select "myColumn", ...
   */
  public String getLiteralSuffix() {


    return null;


  }

  /**
   * @return parameters used in creating the type (may be null)
   */
  public String getCreateParams() {

    return null;

  }

  /**
   * @return can you use null for this type
   */
  public Short getNullable() {

    return null;

  }

  /**
   * @return is it case sensitive
   */
  public Boolean getCaseSensitive() {

    return null;

  }

  /**
   * @return can you use "WHERE" based on this type:
   */
  public Short getSearchable() {

    return null;

  }

  /**
   * @return is it unsigned
   */
  public Boolean getUnsignedAttribute() {

    return null;

  }

  /**
   * @return can it be a money value.
   */
  public Boolean getFixedPrecScale() {

    return null;

  }

  /**
   * @return can it be used for an auto-increment value.
   */
  public Boolean getAutoIncrement() {

    return null;

  }

  /**
   * @return localized version of type name (may be null)
   */
  public String getLocalTypeName() {

    return null;

  }

  /**
   * @return minimum scale supported
   */
  public Integer getMinimumScale() {

    return null;

  }

  /**
   * @return maximum scale supported
   * 0 if unknown
   */
  public Integer getMaximumScale() {

    return 0;

  }



  @Override
  public String toString() {
    return "DataType{" +
      "typeName='" + getTypeName() + '\'' +
      ", typeCode=" + getTypeCode() +
      '}';
  }
}
