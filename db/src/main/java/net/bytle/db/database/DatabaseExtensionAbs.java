package net.bytle.db.database;


import net.bytle.db.model.TableDef;

import java.util.List;

public abstract class DatabaseExtensionAbs implements DatabaseExtension {

    private final Database database;

    public DatabaseExtensionAbs(Database database) {
        this.database = database;
    }

    /**
     * Return a DataTypeDatabase for the corresponding typeCode
     *
     * @param typeCode
     * @return DataTypeDatabase
     */
    @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return null;
    }

    /**
     * Create a "CREATE TABLE" statement for the resultSet with the tableAttributes
     *
     * @param tableDef
     * @return
     */
    @Override
    public List<String> getCreateTableStatements(TableDef tableDef, String name) {
        return null;
    }

    /**
     * Return a java object ready to be loaded into the target driver
     *
     * @param targetColumnType the JDBC data type
     * @param sourceObject     an Java Object to be loaded
     * @return an Object generally ready to be loaded by the driver
     */
    @Override
    public Object getLoadObject(int targetColumnType, Object sourceObject) {
        return null;
    }

    /**
     * Return a normative object name (if _ is not authorized it will be replace by another term for instance)
     *
     * @param objectName
     * @return
     */
    @Override
    public String getNormativeSchemaObjectName(String objectName) {
        return null;
    }

    /**
     * @return the number of concurrent writer connection
     * Example: Sqlite database can only writen by one connection but can be read by many.
     * In this case, {@link #getMaxWriterConnection} will return 1
     */
    @Override
    public Integer getMaxWriterConnection() {
        return null;
    }

    /**
     * Due to a bug in JDBC
     * <p>
     * (ie SQLLite JDBC - the primary column names had the foreign key - it seems that they parse the create statement)
     * We created this hack
     *
     * @param tableDef
     * @return true if implemented / false or null if not implemented
     */
    public Boolean addPrimaryKey(TableDef tableDef) {
        return false;
    }


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
    public Boolean addForeignKey(TableDef tableDef) {
        return false;
    }

}
