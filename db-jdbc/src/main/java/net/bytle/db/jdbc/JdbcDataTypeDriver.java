package net.bytle.db.jdbc;




import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.model.DataTypeInterface;

import java.sql.DatabaseMetaData;

/**
 * Created by gerard on 28-11-2015.
 * An object representing the data of the driver
 * that comes from
 * {@link DatabaseMetaData#getTypeInfo()}
 */
public class JdbcDataTypeDriver implements DataTypeInterface {


    /**
     * The data type code and also the primary key
     */
    private final int dataType;
    private final String typeName;
    private final int precision;
    private final String literalPrefix;
    private final String literalSuffix;
    private final String createParams;
    private final Short nullable;
    private final Boolean caseSensitive;
    private final Short searchable;
    private final Boolean unsignedAttribute;
    private final Boolean fixedPrecScale;
    private final Boolean autoIncrement;
    private final String localTypeName;
    private final Integer minimumScale;
    private final Integer maximumScale;
    private final DataTypeJdbc dataTypeJdbcExtension;

    public JdbcDataTypeDriver(DataTypeInfoBuilder dataTypeInfoBuilder) {
        this.dataType = dataTypeInfoBuilder.dataType;
        this.typeName = dataTypeInfoBuilder.typeName;
        this.precision = dataTypeInfoBuilder.precision;
        this.literalPrefix = dataTypeInfoBuilder.literalPrefix;
        this.literalSuffix = dataTypeInfoBuilder.literalSuffix;
        this.createParams = dataTypeInfoBuilder.createParams;
        this.nullable = dataTypeInfoBuilder.nullable;
        this.caseSensitive = dataTypeInfoBuilder.caseSensitive;
        this.searchable = dataTypeInfoBuilder.searchable;
        this.unsignedAttribute = dataTypeInfoBuilder.unsignedAttribute;
        this.fixedPrecScale = dataTypeInfoBuilder.fixedPrecScale;
        this.autoIncrement = dataTypeInfoBuilder.autoIncrement;
        this.localTypeName = dataTypeInfoBuilder.localTypeName;
        this.minimumScale = dataTypeInfoBuilder.minimumScale;
        this.maximumScale = dataTypeInfoBuilder.maximumScale;
        this.dataTypeJdbcExtension = dataTypeInfoBuilder.dataTypeJdbcExtension;
    }

    public int getTypeCode() {
        return dataType;
    }

    public String getTypeName() {
        return typeName;
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
    public int getMaxPrecision() {
        return precision;
    }

    /**
     * @return prefix used to quote a literal (may be null)
     */
    public String getLiteralPrefix() {
        return literalPrefix;
    }

    /**
     * @return suffix used to quote a literal (may be null)
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
        return Integer.valueOf(maximumScale);
    }


    public static class DataTypeInfoBuilder {

        private final int dataType;
        private String typeName;
        private int precision; // maximum precision
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
        private DataTypeJdbc dataTypeJdbcExtension;


        public DataTypeInfoBuilder(int dataType) {
            this.dataType = dataType;
        }

        public void typeName(String typeName) {
            this.typeName = typeName;
        }

        /**
         * The PRECISION column represents the maximum column size that the server supports for the given datatype.
         * For numeric data, this is the maximum precision.
         * For character data, this is the length in characters.
         * For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component).
         * For binary data, this is the length in bytes.
         * For the ROWID datatype, this is the length in bytes.
         * Null is returned for data types where the column size is not applicable.
         *
         * @param precision
         */
        public void maxPrecision(int precision) {
            this.precision = precision;
        }

        public void literalPrefix(String literalPrefix) {
            this.literalPrefix = literalPrefix;
        }

        public void literalSuffix(String literalSuffix) {
            this.literalSuffix = literalSuffix;
        }

        public void createParams(String createParams) {
            this.createParams = createParams;
        }

        public void nullable(Short nullable) {
            this.nullable = nullable;
            // typeNoNulls - does not allow NULL values
            // typeNullable - allows NULL values
            // typeNullableUnknown - nullability unknown
        }

        public void caseSensitive(Boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public void searchable(Short searchable) {
            this.searchable = searchable;
            // typePredNone - No support
            // typePredChar - Only supported with WHERE .. LIKE
            // typePredBasic - Supported except for WHERE .. LIKE
            // typeSearchable - Supported for all WHERE ..
        }

        public void unsignedAttribute(Boolean unsignedAttribute) {
            this.unsignedAttribute = unsignedAttribute;
        }

        public void fixedPrecScale(Boolean fixedPrecScale) {
            this.fixedPrecScale = fixedPrecScale;
        }

        public void autoIncrement(Boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public void localTypeName(String localTypeName) {
            this.localTypeName = localTypeName;
        }

        public void minimumScale(Integer minimumScale) {
            this.minimumScale = minimumScale;
        }

        public void maximumScale(Integer maximumScale) {
            this.maximumScale = maximumScale;
        }

        public JdbcDataTypeDriver build() {
            return new JdbcDataTypeDriver(this);
        }

    }

    @Override
    public String toString() {
        return "DataTypeDriver{" +
                "typeName='" + typeName + '\'' +
                ", dataType=" + dataType +
                '}';
    }


}
