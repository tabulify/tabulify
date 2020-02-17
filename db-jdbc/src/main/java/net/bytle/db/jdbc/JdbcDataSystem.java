package net.bytle.db.jdbc;

import net.bytle.db.database.DataStore;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * This is a static class
 * No data in there please
 */
public class JdbcDataSystem extends TableSystem {


  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDataSystem.class);


  private JdbcDataProcessingEngine processingEngine;


  /**
   * @param dataPath
   * @return if the table exist in the underlying database (actually the letter case is important)
   * <p>
   */

  @Override
  public Boolean exists(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;

    switch (jdbcDataPath.getType()) {
      case JdbcDataPath.QUERY_TYPE:
        return true;
      default:
        boolean tableExist;
        String[] types = {"TABLE", "VIEW"};

        final String schemaPattern = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
        try (
          ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection()
            .getMetaData()
            .getTables(
              jdbcDataPath.getCatalog(),
              schemaPattern,
              jdbcDataPath.getName(),
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
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    return jdbcDataPath.getSelectStream();
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
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    return jdbcDataPath.isDocument();
  }


  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {

    return Jdbcs.getDescendants((JdbcDataPath) dataPath,null);

  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {

    return Jdbcs.getDescendants((JdbcDataPath) dataPath, glob);

  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    return Jdbcs.getReferencingDataPaths((JdbcDataPath) dataPath);
  }


  @Override
  public DataStore createDataStore(String name, String url) {
    return new JdbcDataStore(name, url, this);
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return !isDocument(dataPath);
  }

  @Override
  public void create(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;

    // Check that the foreign tables exist
    for (ForeignKeyDef foreignKeyDef : dataPath.getDataDef().getForeignKeys()) {
      DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
      if (!exists(foreignDataPath)) {
        throw new RuntimeException("The foreign table (" + foreignDataPath.toString() + ") does not exist");
      }
    }

    // Standard SQL
    List<String> createTableStatements = DbDdl.getCreateTableStatements(dataPath);
    for (String createTableStatement : createTableStatements) {
      try {

        Statement statement = jdbcDataPath.getDataStore().getCurrentConnection().createStatement();
        statement.execute(createTableStatement);
        statement.close();

      } catch (SQLException e) {
        System.err.println(createTableStatement);
        throw new RuntimeException(e);
      }
    }
    final String name = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : "null";
    JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") created in the schema (" + name + ")");


  }

  @Override
  public SqlDataType getDataType(Integer typeCode) {
    throw new RuntimeException("Should not be there clearly");
  }


  @Override
  public void drop(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    StringBuilder dropTableStatement = new StringBuilder();
    dropTableStatement.append("drop ");
    switch (jdbcDataPath.getType()) {
      case JdbcDataPath.TABLE_TYPE:
        dropTableStatement.append("table ");
        break;
      case JdbcDataPath.VIEW_TYPE:
        dropTableStatement.append("view ");
        break;
      default:
        throw new RuntimeException("The drop of the table type (" + jdbcDataPath.getType() + ") is not implemented");
    }
    dropTableStatement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    try (
      Statement statement = jdbcDataPath.getDataStore().getCurrentConnection().createStatement()
    ) {

      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Trying to drop " + jdbcDataPath.getType() + " " + dataPath.toString());
      statement.execute(dropTableStatement.toString());
      JdbcDataSystemLog.LOGGER_DB_JDBC.info(jdbcDataPath.getType() + " " + dataPath.toString() + " dropped");

    } catch (SQLException e) {
      String msg = Strings.multiline( "Dropping of the data path ("+jdbcDataPath+") was not successful with the statement `"+dropTableStatement.toString()+"`"
        , "Cause: "+e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg, e);
    }

  }

  @Override
  public void delete(DataPath dataPath) {


    String deleteStatement = "delete from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
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

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    final JdbcDataStoreExtension sqlDatabase = jdbcDataPath.getDataStore().getExtension();
    String truncateStatement;
    if (sqlDatabase != null) {
      truncateStatement = sqlDatabase.getTruncateStatement(jdbcDataPath);
    } else {
      StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
      truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
      truncateStatement = truncateStatementBuilder.toString();
    }

    try (
      Statement statement = jdbcDataPath.getDataStore().getCurrentConnection().createStatement();
    ) {

      statement.execute(truncateStatement);
      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") truncated");

    } catch (SQLException e) {
      System.err.println(truncateStatement);
      throw new RuntimeException(e);
    }

  }


  @Override
  public InsertStream getInsertStream(DataPath dataPath) {
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    return SqlInsertStream.of(jdbcDataPath);
  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {


    return Jdbcs.getChildrenDataPath((JdbcDataPath) dataPath);

  }

  /**
   * This function is called by {@link net.bytle.db.spi.Tabulars#move(DataPathAbs, DataPathAbs)}
   * The checks on source and target are already done on the calling function
   *
   * @param source
   * @param target
   * @param transferProperties
   */
  @Override
  public void move(DataPath source, DataPath target, TransferProperties transferProperties) {

    // insert into select statement
    JdbcDataPath sourceJdbcDataPath = (JdbcDataPath) source;
    String insertInto = DbDml.getInsertIntoStatement(sourceJdbcDataPath, (JdbcDataPath) target);
    try {
      Statement statement = sourceJdbcDataPath.getDataStore().getCurrentConnection().createStatement();
      Boolean resultSetReturned = statement.execute(insertInto);
      if (!resultSetReturned) {
        int updateCount = statement.getUpdateCount();
        JdbcDataSystemLog.LOGGER_DB_JDBC.info(updateCount + " records where moved from (" + source.toString() + ") to (" + target.toString() + ")");
      }
    } catch (SQLException e) {
      final String msg = "Error when executing the insert into statement: " + insertInto;
      JdbcDataSystemLog.LOGGER_DB_JDBC.severe(msg);
      throw new RuntimeException(msg, e);
    }

  }

}
