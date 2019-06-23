package net.bytle.db.model;

/**
 * Created by gerard on 29-11-2015
 * Just to be able to use the three datatype type (from jdbc, from the driver, for each vendor)
 * in one variable
 */
public interface DataTypeInterface {

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

}
