package net.bytle.db.database;

/**
 * Created by gerard on 4-5-2015.
 * The representation of the JDBC data type.
 *
 */
public interface DataTypeJdbc {

    /**
     * @return the type code (or type number)
     *
     * */
    int getTypeCode();


    /**
     * @return the type name
     *
     * */
    String getTypeName();


    /**
     *
     * @return the java class that may contains this data
     */
    Class<?> getJavaDataType();



}
