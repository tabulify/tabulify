package net.bytle.db.database;

import net.bytle.db.model.TableDef;

import java.util.List;

/**
 * Created by gerard on 28-11-2015.
 * Represents a database vendor.
 *
 *
 * The term "database" is used generically to refer to both the driver and the DBMS.
 *
 * Within a pattern String in the databaseMetadata function, "%" means match any substring of 0 or more characters, and "_" means match any one character.
 */
public interface DatabaseExtension {


    /**
     * Return a DataTypeDatabase for the corresponding typeCode
     * @param typeCode
     * @return DataTypeDatabase
     */
    DataTypeDatabase dataTypeOf(Integer typeCode);

    /**
     * Returns statement to create the table
     * @param tableDef
     * @return
     */
    List<String> getCreateTableStatements(TableDef tableDef, String name);


    /**
     * Return a java object ready to be loaded into the target driver
     * @param targetColumnType the JDBC data type
     * @param sourceObject an Java Object to be loaded
     * @return an Object generally ready to be loaded by the driver
     */
    Object getLoadObject(int targetColumnType, Object sourceObject);

    /**
     * Return a normative object name (if _ is not authorized it will be replace by another term for instance)
     * @param objectName
     * @return
     */
    String getNormativeSchemaObjectName(String objectName);

    /**
     *
     * @return the number of concurrent writer connection
     * Example: Sqlite database can only writen by one connection but can be read by many.
     * In this case, {@link #getMaxWriterConnection} will return 1
     */
    Integer getMaxWriterConnection();


    /**
     * Due to a bug in SQlite JDBC
     * <p>
     * (ie SQLLite JDBC - the primary column names had the foreign key - it seems that they parse the create statement)
     * We created this hack
     *
     * @param tableDef
     * @return true if implemented / false or null if not implemented
     */
    Boolean addPrimaryKey(TableDef tableDef);


    /**
     * SQLite JDBC seems to not have a name for each foreign keys
     * You get an empty string
     * This is not a problem for SQLite
     * but the name is mandatory inside Bytle
     * This hack lets extension implements their own logic
     * <p>
     *
     * @param tableDef
     * @return true if implemented / false or null if not implemented
     */
    Boolean addForeignKey(TableDef tableDef);

}
