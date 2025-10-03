package com.tabulify.jdbc;

import com.tabulify.fs.sql.SqlStatement;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.StrictException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import net.bytle.type.Strings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Static utility to return a select stream
 */
public class SqlRequestExecution {


  public static final String COUNT_COLUMN_NAME = "count";
  public static final String ERROR_CODE_COLUMN_NAME = "error_code";
  public static final String ID_COLUMN_NAME = "id";
  private static final String ERROR_MESSAGE_COLUMN_NAME = "error_message";
  private static final String LINE_COLUMN_NAME = "line";


  static public SelectStream executeAndGetSelectStream(SqlRequest sqlRequest) throws SelectException {


    SqlScript sqlScript = sqlRequest.getExecutableSqlScript();

    SelectStream selectStreamResult = null;


    InsertStream noResultSetResults = sqlRequest.getConnection()
      .getTabular().getMemoryConnection().getDataPath(sqlRequest.getLogicalName())
      .createEmptyRelationDef()
      .addColumn(ID_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
      .addColumn(COUNT_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
      .addColumn("statement", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn(LINE_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
      .addColumn(ERROR_CODE_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
      .addColumn(ERROR_MESSAGE_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
      .getDataPath()
      .getInsertStream();
    List<SqlStatement> executableStatements = sqlScript.getExecutableStatements();
    for (int i = 0; i < executableStatements.size(); i++) {
      SqlStatement sqlStatement = executableStatements.get(i);
      SelectStream statementSelectStreamResult = getSqlStreamResultSet(sqlRequest, sqlStatement);
      /**
       * We support only one callable with an out parameter
       */
      if (sqlRequest.getHasOutParameters()) {
        selectStreamResult = statementSelectStreamResult;
        break;
      }
      /**
       * We support only one result set
       */
      if (statementSelectStreamResult instanceof SqlResultSetStream) {
        selectStreamResult = statementSelectStreamResult;
        break;
      }
      statementSelectStreamResult.next();
      // Update Count may be null
      Integer updateCount = statementSelectStreamResult.getInteger(COUNT_COLUMN_NAME);
      Integer errorCode = statementSelectStreamResult.getInteger(ERROR_CODE_COLUMN_NAME);
      String errorMessage = statementSelectStreamResult.getString(ERROR_MESSAGE_COLUMN_NAME);
      statementSelectStreamResult.close();

      noResultSetResults.insert(i + 1, updateCount, ensureMaxLength(sqlStatement.getStatement(), 50), sqlStatement.getLine(), errorCode, errorMessage);
      // Stop at the first failure
      if (errorCode != 0) {
        break;
      }
    }

    if (selectStreamResult != null) {
      return selectStreamResult;
    }

    return noResultSetResults.getDataPath().getSelectStream();


  }

  public static String ensureMaxLength(String str, int maxLength) {
    if (str == null) return null;
    return str.length() <= maxLength ? str : str.substring(0, maxLength);
  }

  static SelectStream getSqlStreamResultSet(SqlRequest dataPath, SqlStatement sqlStatement) throws SelectException {
    SqlResultSetStream sqlResultSetStream;
    SqlConnection sqlConnection = dataPath.getConnection();
    Connection jdbcConnection = sqlConnection.getCurrentJdbcConnection();
    String statementString = sqlStatement.getStatement();

    InsertStream insertStream = dataPath.getConnection().getTabular().getMemoryConnection().getDataPath(dataPath.getLogicalName())
      .createEmptyRelationDef()
      .addColumn(COUNT_COLUMN_NAME, Integer.class)
      .addColumn(ERROR_CODE_COLUMN_NAME, Integer.class)
      .addColumn(ERROR_MESSAGE_COLUMN_NAME)
      .getDataPath()
      .getInsertStream();
    try {

      boolean result;
      Statement statement;
      try {
        if (dataPath.isParametrizedStatement()) {
          if (dataPath.getHasOutParameters()) {
            /**
             * The CallableStatement interface extends PreparedStatement with methods
             * for executing and retrieving results from stored procedures.
             * Section 13.3 of the JDBC 4.1 Specification JSR 221
             */
            statement = jdbcConnection.prepareCall(statementString);
            CallableStatement callableStatement = (CallableStatement) statement;
            List<SqlParameter> parameters = dataPath.getParameters();
            for (int i = 1; i <= parameters.size(); i++) {
              // for nullability, this should be used
              // preparedStatement.setObject(i, value, type);
              SqlParameter sqlParameter = parameters.get(i - 1);
              switch (sqlParameter.getDirection()) {
                case IN:
                  callableStatement.setObject(i, sqlParameter.getValue(), sqlParameter.getType().getVendorTypeNumber());
                  break;
                case OUT:
                  callableStatement.registerOutParameter(i, sqlParameter.getType().getVendorTypeNumber());
                  break;
                case INOUT:
                  callableStatement.setObject(i, sqlParameter.getValue(), sqlParameter.getType().getVendorTypeNumber());
                  callableStatement.registerOutParameter(i, sqlParameter.getType().getVendorTypeNumber());
                  break;
                default:
                  throw new InternalError("The sql parameter direction " + sqlParameter.getDirection() + " is not in the switch branch");
              }
            }
            result = callableStatement.execute();

          } else {
            statement = jdbcConnection.prepareStatement(statementString);
            PreparedStatement preparedStatement = (PreparedStatement) statement;

            for (SqlParameter sqlParameter : dataPath.getParameters()) {
              // for nullability, this should be used
              // preparedStatement.setObject(i, value, type);
              preparedStatement.setObject(sqlParameter.getIndex(), sqlParameter.getValue(), sqlParameter.getType().getVendorTypeNumber());
            }
            result = preparedStatement.execute();
          }

        } else {
          statement = jdbcConnection.createStatement();
          result = statement.execute(statementString);
        }
      } catch (SQLException e) {
        if (dataPath.getIsStrictExecution()) {
          throw new StrictException("A sql statement throws an error.\nError: " + e.getMessage() + ".\nData Resource: " + dataPath + "\nLine: " + sqlStatement.getLine() + "\nSQL: " + sqlStatement.getStatement(), e);
        }
        return insertStream
          .insert(null, 1, e.getMessage())
          .getDataPath()
          .getSelectStream();
      }

      /**
       * Warning
       * Example: PRINT statement of SQL Server
       * https://stackoverflow.com/questions/1759801/is-there-a-way-to-display-print-results-with-sql-server-jdbc-driver
       */
      SQLWarning warning = statement.getWarnings();
      while (warning != null) {
        System.err.println(warning.getMessage());
        warning = warning.getNextWarning();
      }


      /**
       * No Result set ?
       */
      if (!result) {

        /**
         * Procedure with `OUT` parameters?
         */
        if (dataPath.getHasOutParameters()) {
          RelationDef relationDef = dataPath.getConnection().getTabular().getMemoryConnection().getDataPath(dataPath.getLogicalName())
            .createEmptyRelationDef();
          List<Object> values = new ArrayList<>();
          for (SqlParameter sqlParameter : dataPath.getParameters()) {
            if (sqlParameter.getDirection().equals(SqlParameterDirection.IN)) {
              continue;
            }
            relationDef.addColumn(sqlParameter.getName().toSqlCase(), sqlParameter.getType());
            values.add(((CallableStatement) statement).getObject(sqlParameter.getIndex()));
          }

          return relationDef
            .getDataPath()
            .getInsertStream()
            .insert(values)
            .getDataPath()
            .getSelectStream();
        }

        /**
         * No out parameters
         */
        int updateCount = statement.getUpdateCount();

        return insertStream
          .insert(updateCount, 0, null)
          .getDataPath()
          .getSelectStream();

      }
      // First result set
      sqlResultSetStream = new SqlResultSetStream(dataPath, statement.getResultSet(), sqlStatement);
      // Get next result set
      // By default, it will close the previous one implicitly
      // We need to encapsulate a statement
      try {
        if (statement.getMoreResults(Statement.KEEP_CURRENT_RESULT)) {
          // ResultSet rs2 = statement.getResultSet();
          throw new UnsupportedOperationException("We got multiple result set, multiple result Sql Query are not supported yet");
        }
      } catch (SQLException e) {
        // Why we catch?
        // SQL Server does not support it and throws
        // com.microsoft.sqlserver.jdbc.SQLServerException: This operation is not supported.
      }
      return sqlResultSetStream;

    } catch (SQLException e) {
      String message = "An error has occurred executing a sql statement of the sql resource (" + dataPath + ") Error: " + e.getMessage() + Strings.EOL + " Statement: " + statementString;
      if (!dataPath.isParametrizedStatement() && (statementString.contains("?") || statementString.contains("$1"))) {
        message = message + "\nIt seems that the statement is a prepared statement but no parameters were set.";
      }
      throw new SelectException(message, e);
    }

  }

}
