package net.bytle.db.jdbc;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.model.TableDef;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * This class is a hookup for database when:
 *   * the driver is failing us
 *   * and when the SQL is not ISO
 */
public abstract class JdbcDataStoreExtension {


  private final JdbcDataStore jdbcDataStore;

  public JdbcDataStoreExtension(JdbcDataStore jdbcDataStore) {
    this.jdbcDataStore = jdbcDataStore;
  }

  /**
   * Return a DataTypeDatabase for the corresponding typeCode
   *
   * @param typeCode
   * @return DataTypeDatabase
   */
  public abstract DataTypeDatabase dataTypeOf(Integer typeCode);

  /**
   * Returns statement to create the table
   *
   * @param dataPath
   * @return
   */
  protected List<String> getCreateTableStatements(JdbcDataPath dataPath){
    return new ArrayList<>();
  }


  /**
   * Return a java object ready to be loaded into the target driver
   *
   * @param targetColumnType the JDBC data type
   * @param sourceObject     an Java Object to be loaded
   * @return an Object generally ready to be loaded by the driver
   */
  protected Object getLoadObject(int targetColumnType, Object sourceObject){
    return this.jdbcDataStore.getLoadObject(targetColumnType,sourceObject);
  }

  /**
   * Return a normative object name (if _ is not authorized it will be replace by another term for instance)
   *
   * @param objectName
   * @return
   */
  protected abstract String getNormativeSchemaObjectName(String objectName);

  /**
   * @return the number of concurrent writer connection
   * Example: Sqlite database can only writen by one connection but can be read by many.
   * In this case, {@link #getMaxWriterConnection} will return 1
   */
  protected abstract Integer getMaxWriterConnection();


  /**
   * Due to a bug in SQlite JDBC
   * <p>
   * (ie SQLLite JDBC - the primary column names had the foreign key - it seems that they parse the create statement)
   * We created this hack
   *
   * @param tableDef
   * @return true if implemented / false or null if not implemented
   */
  protected Boolean addPrimaryKey(TableDef tableDef) {
    return false;
  }


  /**
   * SQLite JDBC seems to not have a name for each foreign keys
   * You of an empty string
   * This is not a problem for SQLite
   * but the name is mandatory inside Bytle
   * This hack lets extension implements their own logic
   * <p>
   *
   * @param tableDef
   * @return true if implemented / false or null if not implemented
   */
  protected Boolean addForeignKey(TableDef tableDef) {
    return false;
  }

  /**
   * Add columns to a table
   * This function was created because Sqlite does not really implements a JDBC type
   * Sqlite gives them back via a string
   *
   * @param tableDef
   * @return true if the columns were added to the table
   */
  protected boolean addColumns(TableDef tableDef){
    return false;
  }

  public abstract String getTruncateStatement(JdbcDataPath dataPath);


}
