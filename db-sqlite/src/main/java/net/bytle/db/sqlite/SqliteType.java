package net.bytle.db.sqlite;


import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.log.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that takes the value of the column type from the PRAGMA table_info();
 * and return type information (typeName, Precision and scale)
 *
 */
public class SqliteType {

    private static final Log LOGGER = Sqlites.LOGGER_SQLITE;

    String type;
    Integer scale;
    Integer precision;

    private SqliteType(String type, Integer precision, Integer scale) {
        this.type = type;
        this.scale = scale;
        this.precision = precision;
    }

    /**
     * @param description - A datatype string definition in the form:
     *                    * type(precision, scale)
     *                    * type(precision)
     *                    * type
     * @return a data type
     * <p>
     * Example: INTEGER(50,2)
     */

    static public SqliteType get(String description) {
        Pattern pattern = Pattern.compile("\\s*([a-zA-Z]+)\\s*(?:\\(([0-9,]+)\\))?\\s*");
        Matcher matcher = pattern.matcher(description);
        String typeName = null;
        Integer scale = null;
        Integer precision = null;
        while (matcher.find()) {

            typeName = matcher.group(1);
            String scaleAndPrecision = matcher.group(2);
            if (scaleAndPrecision != null) {
                String[] array = scaleAndPrecision.split(",");
                precision = Integer.valueOf(array[0]);
                if (array.length == 2) {
                    scale = Integer.valueOf(array[1]);
                }
            }

        }
        return new SqliteType(typeName, precision, scale);
    }

    public String getTypeName() {
        return type;
    }

    public Integer getTypeCode() {
        final DataTypeJdbc of = DataTypesJdbc.of(type);
        if (of == null) {
            LOGGER.warning("The type code is unknown for the type (" + type + ")");
            return null;
        } else {
            return of.getTypeCode();
        }
    }

    public Integer getScale() {
        return this.scale;
    }

    public Integer getPrecision() {
        return this.precision;
    }
}
