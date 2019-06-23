package net.bytle.db.model;


import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a data type definition
 * Typee with two ee because DataType was already taken
 */
public class DataTypee {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    String type;
    Integer scale;
    Integer precision;

    private DataTypee(String type, Integer precision, Integer scale) {
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

    static public DataTypee get(String description) {
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
        return new DataTypee(typeName, precision, scale);
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
