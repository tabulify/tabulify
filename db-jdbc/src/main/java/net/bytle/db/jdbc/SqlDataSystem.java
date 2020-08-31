package net.bytle.db.jdbc;

import net.bytle.db.database.DataStore;
import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferListenerAtomic;
import net.bytle.db.transfer.TransferSourceTarget;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * An ANSI SQL system based on JDBC
 */
public class SqlDataSystem implements DataSystem {


  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDataSystem.class);


  /**
   * The data store is needed to get the connection to be able to
   * for instance {@link #execute(List)} statements
   */
  private SqlDataStore jdbcDataStore;


  public SqlDataSystem(SqlDataStore jdbcDataStore) {
    this.jdbcDataStore = jdbcDataStore;
  }

  /**
   * @param dataPath
   * @return if the table exist in the underlying database (actually the letter case is important)
   * <p>
   */

  @Override
  public Boolean exists(DataPath dataPath) {

    SqlDataPath jdbcDataPath = (SqlDataPath) dataPath;

    SqlDataPath.Type type = SqlDataPath.Type.fromString(jdbcDataPath.getType());
    switch (type) {
      case QUERY:
        return true;
      default:
        boolean tableExist;
        String[] types = {"TABLE", "VIEW"};

        final String schemaPattern = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
        String catalog = jdbcDataPath.getCatalog();
        String name = jdbcDataPath.getName();
        try (
          ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection()
            .getMetaData()
            .getTables(
              catalog,
              schemaPattern,
              name,
              types)
        ) {
          tableExist = tableResultSet.next(); // For TYPE_FORWARD_ONLY
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        return tableExist;
    }


  }

  @Override
  public SelectStream getSelectStream(DataPath dataPath) {
    SqlDataPath jdbcDataPath = (SqlDataPath) dataPath;
    return jdbcDataPath.getOrCreateDataDef().getSelectStream();
  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {

    throw new UnsupportedOperationException("Not implemented");

  }

  @Override
  public long size(DataPath dataPath) {

    long size = 0;
    DataPath queryDataPath = dataPath.getDataStore().getQueryDataPath("select count(1) from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    try (
      SelectStream selectStream = getSelectStream(queryDataPath)
    ) {
      Boolean next = selectStream.next();
      if (next) {
        size = selectStream.getInteger(0);
      }
    }
    return size;
  }


  @Override
  public boolean isDocument(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    return sqlDataPath.isDocument();
  }


  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target) {

    TransferSourceTarget transferSourceTarget = new TransferSourceTarget(source, target);
    // Check the source
    transferSourceTarget.checkSource();

    // Check the target
    transferSourceTarget.createOrCheckTargetFromSource();

    // The target table should be without rows
    long size = Tabulars.getSize(target);
    assert size == 0 : "In a copy operation, the target table should be empty. This is not the case. The target table (" + target + ") has (" + size + ") rows";

    // Insert into select statement
    SqlDataPath sourceJdbcDataPath = (SqlDataPath) source;
    String insertInto = DbDml.getInsertIntoStatement(sourceJdbcDataPath, (SqlDataPath) target);
    TransferListenerAtomic transferListenerStream = new TransferListenerAtomic(transferSourceTarget);
    try {
      Statement statement = sourceJdbcDataPath.getDataStore().getCurrentConnection().createStatement();
      Boolean resultSetReturned = statement.execute(insertInto);
      if (!resultSetReturned) {
        int updateCount = statement.getUpdateCount();
        transferListenerStream.incrementRows(updateCount);
        JdbcDataSystemLog.LOGGER_DB_JDBC.info(updateCount + " records where moved from (" + source.toString() + ") to (" + target.toString() + ")");
      }
    } catch (SQLException e) {
      final String msg = "Error when executing the insert into statement: " + insertInto;
      JdbcDataSystemLog.LOGGER_DB_JDBC.severe(msg);
      transferListenerStream.addException(e);
      throw new RuntimeException(msg, e);
    }
    return transferListenerStream;

  }

  @Override
  public TransferListener insert(DataPath source, DataPath target) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {

    return Ansis.getDescendants((SqlDataPath) dataPath, null);

  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {

    return Ansis.getDescendants((SqlDataPath) dataPath, glob);

  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    return Ansis.getReferencingDataPaths((SqlDataPath) dataPath);
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return !isDocument(dataPath);
  }

  @Override
  public void create(DataPath dataPath) {

    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;

    // Check that the foreign tables exist
    for (ForeignKeyDef foreignKeyDef : dataPath.getOrCreateDataDef().getForeignKeys()) {
      DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
      if (!exists(foreignDataPath)) {
        throw new RuntimeException("The foreign table (" + foreignDataPath.toString() + ") does not exist");
      }
    }

    // Standard SQL
    List<String> createTableStatements = createTableStatements(sqlDataPath);
    this.execute(createTableStatements);
    final String name = sqlDataPath.getSchema() != null ? sqlDataPath.getSchema().getName() : "null";
    JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") created in the schema (" + name + ")");


  }

  /**
   * Return all statements (inclusive primary key, foreign key and unique key)
   * to create a table
   *
   * @param dataPath The table Name in the create statement
   */
  protected List<String> createTableStatements(SqlDataPath dataPath) {

    List<String> statements = new ArrayList<>();
    String createTableStatement = "create table " +
      JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath) +
      " (\n" +
      createColumnsStatement(dataPath) +
      " )\n";
    statements.add(createTableStatement);

    // Primary Key
    final PrimaryKeyDef primaryKey = dataPath.getOrCreateDataDef().getPrimaryKey();
    if (primaryKey != null) {
      if (primaryKey.getColumns().size() != 0) {
        String createPrimaryKeyStatement = createPrimaryKeyStatement(dataPath);
        if (createPrimaryKeyStatement != null) {
          statements.add(createPrimaryKeyStatement);
        }
      }
    }

    // Foreign key
    for (ForeignKeyDef foreignKeyDef : dataPath.getOrCreateDataDef().getForeignKeys()) {
      String createForeignKeyStatement = createForeignKeyStatement(foreignKeyDef);
      if (createForeignKeyStatement != null) {
        statements.add(createForeignKeyStatement);
      }
    }

    // Unique key
    for (UniqueKeyDef uniqueKeyDef : ((SqlDataPath) dataPath).getOrCreateDataDef().getUniqueKeys()) {
      String createUniqueKeyStatement = createUniqueKeyStatement(uniqueKeyDef);
      statements.add(createUniqueKeyStatement);
    }

    return statements;

  }

  /**
   * @param dataPath : The target schema
   * @return the column string part of a create statement
   */
  protected String createColumnsStatement(DataPath dataPath) {


    StringBuilder statementColumnPart = new StringBuilder();
    RelationDef dataDef = dataPath.getOrCreateDataDef();
    for (int i = 0; i < dataDef.getColumnsSize(); i++) {

      try {

        ColumnDef columnDef = dataDef.getColumnDef(i);
        // Add it to the columns statement
        statementColumnPart.append(createColumnStatement(columnDef));

      } catch (Exception e) {

        throw new RuntimeException(e + "\nException: The Column Statement build until now is:\n" + statementColumnPart, e);

      }

      // Is it the end ...
      if (i != dataDef.getColumnsSize() - 1) {
        statementColumnPart.append(",\n");
      } else {
        statementColumnPart.append("\n");
      }

    }

    return statementColumnPart.toString();

  }

  protected String createDataTypeStatement(String columnTypeName, Integer precision, Integer scale) {

    String dataTypeCreateStatement = columnTypeName;
    if (precision != null && precision > 0) {
      dataTypeCreateStatement += "(" + precision;
      if (scale != null && scale != 0) {
        dataTypeCreateStatement += "," + scale;
      }
      dataTypeCreateStatement += ")";
    }
    return dataTypeCreateStatement;

  }

  /**
   * @param uniqueKeyDef - The source unique key def
   * @return an alter table unique statement
   */
  protected String createUniqueKeyStatement(UniqueKeyDef uniqueKeyDef) {

    String statement = "ALTER TABLE " + JdbcDataSystemSql.getFullyQualifiedSqlName(uniqueKeyDef.getRelationDef().getDataPath()) + " ADD ";

    // The serie of columns definitions (col1, col2,...)
    final List<ColumnDef> columns = uniqueKeyDef.getColumns();
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef columnDef : columns) {
      columnNames.add(columnDef.getColumnName());
    }
    final String columnDefStatement = String.join(",", columnNames.toArray(new String[columnNames.size()]));

    // The final statement that presence of the name
    final String name = uniqueKeyDef.getName();
    if (name == null) {
      statement = statement + "UNIQUE (" + columnDefStatement + ") ";
    } else {
      statement = statement + "CONSTRAINT " + name + " UNIQUE (" + columnDefStatement + ") ";
    }

    return statement;

  }


  /**
   * A blank statement in the form "columnName datatype(scale, precision)"
   * The constraint such as NOT NULL unique may change between database
   * Example Sqlite has the primary key statement before NOT NULL
   *
   * @param columnDef : The source column
   * @return
   */
  protected String createColumnStatement(ColumnDef columnDef) {

    // Target data type from source data type is lost
    // It should be handle when the path is changing of database
    SqlDataType targetSqlDataType = columnDef.getDataType();

    // Always passed to create the statement
    Integer precision = columnDef.getPrecision();
    Integer maxPrecision = targetSqlDataType.getMaxPrecision();
    if (precision == null) {
      precision = maxPrecision;
    }
    if (precision != null && maxPrecision != null && precision > maxPrecision) {
      DataStore dataStore = columnDef.getDataDef().getDataPath().getDataStore();
      String message = "The precision (" + precision + ") of the column (" + columnDef + ") is greater than the maximum allowed (" + maxPrecision + ") for the datastore (" + dataStore.getName() + ")";
      if (dataStore.isStrict()) {
        throw new RuntimeException(message);
      } else {
        LOGGER.warn(message);
        precision = maxPrecision;
      }
    }
    Integer scale = columnDef.getScale();
    if (scale == null) {
      scale = targetSqlDataType.getMaximumScale();
    }

    String dataTypeCreateStatement = null;


    if (targetSqlDataType.getTypeCode() == Types.DATE || targetSqlDataType.getTypeCode() == Types.TIME) {

      dataTypeCreateStatement = targetSqlDataType.getTypeNames().get(0);

    } else {

      dataTypeCreateStatement = createDataTypeStatement(targetSqlDataType.getTypeNames().get(0), precision, scale);

    }

    // NOT NULL
    String notNullStatement = "";
    if (!columnDef.getNullable()) {
      // Hack because hive is read only, it does not support Not Null
      if (!((SqlDataPath) columnDef.getDataDef().getDataPath()).getDataStore().getProductName().equals(SqlDataStore.DB_HIVE)) {
        notNullStatement = " NOT NULL";
      }
    }

    // Hack for Hive
    String encloseString = "\"";
    if (((SqlDataPath) columnDef.getDataDef().getDataPath()).getDataStore().getProductName().equals(SqlDataStore.DB_HIVE)) {
      encloseString = "`";
    }

    // Number as columnName is not possible in Oracle
    // Just with two double quote
    return encloseString + columnDef.getColumnName() + encloseString + " " + dataTypeCreateStatement + notNullStatement;
  }

  /**
   * @param foreignKeyDef - The source foreign key
   * @return a alter table foreign key statement
   */
  protected String createForeignKeyStatement(ForeignKeyDef foreignKeyDef) {

    SqlDataStore jdbcDataSystem = (SqlDataStore) foreignKeyDef.getTableDef().getDataPath().getDataStore();

    // Constraint are supported from 2.1
    // https://issues.apache.org/jira/browse/HIVE-13290
    if (jdbcDataSystem.getProductName().equals(SqlDataStore.DB_HIVE)) {
      if (jdbcDataSystem.getDatabaseMajorVersion() < 2) {
        return null;
      } else {
        if (jdbcDataSystem.getDatabaseMinorVersion() < 1) {
          return null;
        }
      }
    }
    StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
    statement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(foreignKeyDef.getTableDef().getDataPath()))
      .append(" ADD ")
      .append("CONSTRAINT ")
      .append(foreignKeyDef.getName())
      .append(" FOREIGN KEY (");
    final List<ColumnDef> nativeColumns = foreignKeyDef.getChildColumns();
    for (int i = 0; i < nativeColumns.size(); i++) {
      statement.append(nativeColumns.get(i).getColumnName());
      if (i != nativeColumns.size() - 1) {
        statement.append(", ");
      }
    }
    statement.append(") ");


    final DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
    statement
      .append("REFERENCES ")
      .append(JdbcDataSystemSql.getFullyQualifiedSqlName(foreignDataPath))
      .append(" (");
    List<ColumnDef> foreignColumns = foreignDataPath.getOrCreateDataDef().getPrimaryKey().getColumns();
    for (int i = 0; i < foreignColumns.size(); i++) {
      statement.append(foreignColumns.get(i).getColumnName());
      if (i != foreignColumns.size() - 1) {
        statement.append(", ");
      }
    }
    statement.append(")");

    return statement.toString();


  }

  protected String createPrimaryKeyStatement(SqlDataPath jdbcDataPath) {

    final PrimaryKeyDef primaryKey = jdbcDataPath.getOrCreateDataDef().getPrimaryKey();
    List<ColumnDef> columns = primaryKey.getColumns();
    int size = columns.size();
    if (size == 0) {
      return null;
    }

    // TODO: Move to Hive
    // Constraint are supported from 2.1
    // https://issues.apache.org/jira/browse/HIVE-13290
    final SqlDataStore dataStore = jdbcDataPath.getDataStore();
    if (dataStore.getProductName().equals(SqlDataStore.DB_HIVE)) {
      if (dataStore.getDatabaseMajorVersion() < 2) {
        return null;
      } else {
        if (dataStore.getDatabaseMinorVersion() < 1) {
          return null;
        }
      }
    }

    StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
    statement
      .append(JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath))
      .append(" ADD ");
    if (primaryKey.getName() != null) {
      statement
        .append("CONSTRAINT ")
        .append(primaryKey.getName())
        .append(" ");
    }
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef columnDef : columns) {
      columnNames.add(columnDef.getColumnName());
    }
    statement
      .append("PRIMARY KEY  (")
      .append(String.join(", ", columnNames))
      .append(")");

    return statement.toString();
  }


  @Override
  public void drop(DataPath dataPath) {

    SqlDataPath jdbcDataPath = (SqlDataPath) dataPath;
    StringBuilder dropTableStatement = new StringBuilder();
    dropTableStatement.append("drop ");
    SqlDataPath.Type type = SqlDataPath.Type.fromString(jdbcDataPath.getType());
    switch (type) {
      case TABLE:
        dropTableStatement.append("table ");
        break;
      case VIEW:
        dropTableStatement.append("view ");
        break;
      default:
        throw new RuntimeException("The drop of the table type (" + type + ") is not implemented");
    }
    dropTableStatement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    try (
      Statement statement = jdbcDataPath.getDataStore().getCurrentConnection().createStatement()
    ) {

      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Trying to drop " + type + " " + dataPath.toString());
      statement.execute(dropTableStatement.toString());
      JdbcDataSystemLog.LOGGER_DB_JDBC.info(type + " " + dataPath.toString() + " dropped");

    } catch (SQLException e) {
      String msg = Strings.multiline("Dropping of the data path (" + jdbcDataPath + ") was not successful with the statement `" + dropTableStatement.toString() + "`"
        , "Cause: " + e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg, e);
    }

  }

  @Override
  public void delete(DataPath dataPath) {


    String deleteStatement = "delete from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
    SqlDataPath jdbcDataPath = (SqlDataPath) dataPath;
    try (
      Statement statement = jdbcDataPath.getDataStore().getCurrentConnection().createStatement();
    ) {
      statement.execute(deleteStatement);
      // Without commit, the database is locked for sqlite (if the connection is no more in autocommit mode)
      jdbcDataPath.getDataStore().getCurrentConnection().commit();
      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table " + dataPath.getDataStore() + " deleted");
    } catch (SQLException e) {

      throw new RuntimeException(e);
    }

  }

  @Override
  public void truncate(DataPath dataPath) {

    SqlDataPath jdbcDataPath = (SqlDataPath) dataPath;
    StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    String truncateStatement = truncateStatementBuilder.toString();

    try (
      Statement statement = jdbcDataPath.getDataStore().getCurrentConnection().createStatement()
    ) {

      statement.execute(truncateStatement);
      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") truncated");

    } catch (SQLException e) {
      System.err.println(truncateStatement);
      throw new RuntimeException(e);
    }

  }

  protected String truncateStatement(SqlDataPath dataPath) {

    return "truncate table " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);

  }


  @Override
  public InsertStream getInsertStream(DataPath dataPath) {
    SqlDataPath jdbcDataPath = (SqlDataPath) dataPath;
    return SqlInsertStream.of(jdbcDataPath);
  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {

    return Ansis.getChildrenDataPath((SqlDataPath) dataPath);

  }

  /**
   * This function is called by {@link net.bytle.db.spi.Tabulars#move(DataPath, DataPath)}
   * The checks on source and target are already done on the calling function
   *
   * @param source
   * @param target
   */
  @Override
  public void move(DataPath source, DataPath target) {

    throw new RuntimeException("Renaming a table is not yet implemented");

  }

  /**
   * Execute SQL statements
   *
   * @param statements
   */
  public void execute(List<String> statements) {
    for (String statementAsString : statements) {
      execute(statementAsString);
    }
  }

  /**
   * Execute a sql statement
   *
   * @param statement
   */
  public void execute(String statement) {
    try (Statement sqlStatement = this.getDataStore().getCurrentConnection().createStatement()) {
      sqlStatement.execute(statement);
    } catch (SQLException e) {
      String message = Strings.multiline(
        "An error occured executing the following statement:",
        statement);
      LOGGER.error(message);
      throw new RuntimeException(message, e);
    }
  }

  public SqlDataStore getDataStore() {
    return jdbcDataStore;
  }


  /**
   * Return an object to be set in a prepared statement (for instance)
   * before insertion
   *
   * Example:
   *   * if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as a oracle.sql.BINARY_DOUBLE
   *   * if you want to load a string into a bigint, you need to transform it
   *
   * @param targetColumnType
   * @param targetColumnType the target column type
   * @return an object to be loaded
   */
  public Object castLoadObjectIfNecessary(Object sourceObject, int targetColumnType) {

    String clazz = sourceObject.getClass().toString();

    switch (clazz){
      case "java.lang.String":
        String stringSourceObject = (String) sourceObject;
        switch (targetColumnType){
          case (Types.BIGINT):
             sourceObject = new BigInteger(stringSourceObject);
             break;
        }
        break;
    }

    return sourceObject;

  }
}
