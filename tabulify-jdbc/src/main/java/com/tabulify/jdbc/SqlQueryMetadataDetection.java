package com.tabulify.jdbc;

import com.tabulify.DbLoggers;
import com.tabulify.fs.sql.SqlQuery;
import com.tabulify.fs.sql.SqlQueryColumnIdentifierExtractor;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import net.bytle.crypto.Digest;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import static com.tabulify.jdbc.SqlQueryMetadataDetectionMethod.DESCRIBE;
import static com.tabulify.jdbc.SqlQueryMetadataDetectionMethod.TEMPORARY_VIEW;

public class SqlQueryMetadataDetection {


  private final SqlQueryMetadataDetectionBuilder builder;

  public SqlQueryMetadataDetection(SqlQueryMetadataDetectionBuilder sqlQueryMetadataDetectionBuilder) {
    this.builder = sqlQueryMetadataDetectionBuilder;

  }

  public static SqlQueryMetadataDetectionBuilder builder() {
    return new SqlQueryMetadataDetectionBuilder();
  }

  private void detectViaMethod(SqlRequestRelationDef relation, SqlQueryMetadataDetectionMethod metadataDetectionMethod) throws Exception {


    SqlScript executableSqlScript = relation.getDataPath().getExecutableSqlScript();
    String query = executableSqlScript.getSelect();

    /**
     * Query
     * <p>
     * To extract the columns identifier and structure, there is a history of three methods
     *   * the first one was to get the structure by sending the query and retrieving the structure from the {@link DatabaseMetaData}
     *   via the {@link SqlResultSetStream#getRuntimeDataDef(DataDef)}
     *   * then we parsed the script via {@link SqlQueryColumnIdentifierExtractor} because we needed it to create SQL data processing script such as upsert, create as
     *   * then we saw that we could create a temporary view, read the metadata and delete it
     *   * then we saw that the SQL DESCRIBE statement could be used
     *   * then we saw that we could wrap the query with a false equality to not execute it
     * <p>
     */
    Connection jdbcConnection = relation.getDataPath()
      .getConnection()
      .getCurrentJdbcConnection();

    switch (metadataDetectionMethod) {
      case DESCRIBE:
        // statement does not need to have parameters to be prepared
        try (
          PreparedStatement statement = jdbcConnection.prepareStatement(query)
        ) {
          SqlResultSetStream.mergeResultSetMetadata(relation, statement.getMetaData());
        } catch (SQLException e) {
          // Error
          throw new Exception(e);
        }
        return;
      case FALSE_EQUALITY:
        //noinspection SqlDialectInspection
        String falseQualityStatement = "select * from (" + query + ") query where 1=0";
        try (
          PreparedStatement statement = jdbcConnection.prepareStatement(falseQualityStatement)
        ) {
          statement.execute();
          SqlResultSetStream.mergeResultSetMetadata(relation, statement.getMetaData());
        } catch (SQLException e) {
          // Error
          throw new Exception(e);
        }
        return;
      //noinspection ConstantConditions
      case TEMPORARY_VIEW:

        /**
         * query can be other thing than a select
         * Example: `PRAGMA table_info(f_sales)`
         * creating a temporary view will not work because this is not permitted by SQLit
         * not the best, but it will work as we read the columns after query execution.
         */

        if (relation.getDataPath().isParametrizedStatement()) {
          throw new Exception("Prepared statement can't create view");
        }

        /**
         * We don't want to see the creation/dropping
         * of the view in the INFO log
         */
        Level oldLevel = DbLoggers.LOGGER_DB_ENGINE.getLevel();
        SqlLog.LOGGER_DB_JDBC.setLevel(Level.WARNING);
        SqlConnection connection = relation.getDataPath().getConnection();
        SqlDataSystem dataSystem = connection.getDataSystem();
        /**
         * Due to side effect such as recursive call, we create another
         * data path view
         * A drop may ask for foreign key of the runtime data resource
         * creating a recursive call
         */


        String name = "tmp_tabulify_" + Digest.createFromString(Digest.Algorithm.MD5, query).getHashHex();
        SqlDataPath temporaryView = connection.getDataPath(name);

        // Delete if exist
        // the name is not random but deterministic
        // We may get this error:
        // ERROR: relation "tmp_tabulify_95adb6e77a0884d9e50232cb8c5c969d" already exists
        // Why? Because database will store any sql query (even bad one)
        // The view may have been created, we delete it
        Tabulars.dropIfExists(temporaryView);

        try {

          temporaryView = dataSystem.createAsView(executableSqlScript, temporaryView);
          relation.mergeStruct(temporaryView.getOrCreateRelationDef());

        } catch (Exception e) {
          /**
           * Note all select statement can be created as a view
           * You need to use {@link SelectStream#getRuntimeRelationDef()}
           * Otherwise the user may think that the program has an error.
           * For instance, if the transfer was not using {@link SelectStream#getRuntimeRelationDef()} it will fail miserably saying:
           * java.lang.RuntimeException: With the mapping column method (NAME), we cannot create a target because the source ((sqlite/query_11.sql@tpcds_query)@sqlite) has no columns.
           * But a query has a structure, yeah?
           * <p>
           * You can't create a view:
           * * when the column name are the same
           * Example: ERROR: column "avg" specified more than once
           * * Or with an order by (Sql Server)
           * The ORDER BY clause is invalid in views, inline functions, derived tables, subqueries, and common table expressions, unless TOP, OFFSET or FOR XML is also specified.
           * * or with any prepared statement
           */
          throw e;

        } finally {

          Tabulars.dropIfExists(temporaryView);
          SqlLog.LOGGER_DB_JDBC.setLevel(oldLevel);

        }
        break;
      case RUNTIME:
        /**
         * Done by the function/step via {@link SelectStream#getRuntimeRelationDef()}
         */
        break;
      case PARSING:
        /**
         * Weakness
         * This method:
         *   * returns only the identifier not the data type (data type is then VARCHAR for all columns)
         *   * does not work with the star `select * from`
         */
        parseQueryAndAddColumns(relation);
        break;
    }


  }

  /**
   * Parse the query, extract the column identifier
   * and add the columns accordingly
   */
  private void parseQueryAndAddColumns(SqlRequestRelationDef relation) {
    List<String> columnIdentifiers = SqlQuery
      .createFromString(relation.getDataPath().getExecutableSqlScript().getSelect())
      .createColumnIdentifierExtractor()
      .setFunctionNameAsIdentifier(true)
      .setLowerCaseIdentifier(true)
      .extractColumnIdentifiers();
    for (int i = 0; i < columnIdentifiers.size(); i++) {
      String columnName = columnIdentifiers.get(i);
      if (!relation.hasColumn(columnName)) {
        relation.addColumn(columnName);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning("The column name (" + columnName + ") is specified more than once in the query (" + relation.getDataPath() + "). This will surely cause a problem during a data transfer.");
        /**
         * Postgres can return the same name for the columns
         * (ie the name of the function for instance)
         * If we have two avg function, we will get two columns with the same name `avg`
         */
        relation.addColumn(columnName + "-" + i);
      }

    }

  }

  public void detect(SqlRequestRelationDef relation) {

    SqlScript executableSqlScript = relation.getDataPath().getExecutableSqlScript();
    if (!executableSqlScript.isSingleSelectStatement()) {
      return;
    }

    for (SqlQueryMetadataDetectionMethod selectMetadataMethod : this.builder.detectionMethods) {
      try {
        this.detectViaMethod(relation, selectMetadataMethod);
        if (!relation.getColumnDefs().isEmpty()) {
          break;
        }
      } catch (Exception e) {
        // error, we try the next
      }
    }
  }

  public static class SqlQueryMetadataDetectionBuilder {


    private List<SqlQueryMetadataDetectionMethod> detectionMethods;


    public SqlQueryMetadataDetection build() {
      if (detectionMethods == null) {
        detectionMethods = List.of(DESCRIBE, TEMPORARY_VIEW);
      }
      return new SqlQueryMetadataDetection(this);
    }

    public SqlQueryMetadataDetectionBuilder setDetectionMethods(SqlQueryMetadataDetectionMethod sqlQueryMetadataDetectionMethod) {
      this.detectionMethods = List.of(sqlQueryMetadataDetectionMethod);
      return this;
    }

    public SqlQueryMetadataDetectionBuilder setDetectionMethods(List<SqlQueryMetadataDetectionMethod> detectionMethods) {
      this.detectionMethods = detectionMethods;
      return this;
    }
  }
}
