package net.bytle.db.database;

/**
 * Created by gerard on 4-5-2015.
 *
 * An extension mechanism to be able to overrule
 * the driver data type information in order to correct the problem
 */
public interface DataTypeDatabase  {



    /**
     * @return the create statement part
     * */
    String getCreateStatement(int precision, int scale);


    /**
     *
     * @return the java class that may contains this data
     */
    Class<?> getJavaDataType();


    /**
     *
     * @return the vendor class data type implementation
     */
    Class<?> getVendorClass();


    /**
     * The database type name
     * @return
     */
    String getTypeName();
}
