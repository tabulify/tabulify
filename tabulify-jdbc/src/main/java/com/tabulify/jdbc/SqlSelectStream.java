package com.tabulify.jdbc;

import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.exception.CastException;
import net.bytle.type.Strings;

import java.sql.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SqlSelectStream extends SelectStreamAbs implements SelectStream {


  private final SqlConnection jdbcDataStore;

  private final SqlDataPath jdbcDataPath;

  // The cursor
  private ResultSet resultSet;

  private Statement statement;

  public SqlSelectStream(SqlDataPath jdbcDataPath) throws SelectException {

    super(jdbcDataPath);
    this.jdbcDataPath = jdbcDataPath;
    this.jdbcDataStore = jdbcDataPath.getConnection();
    this.getResultSet();


  }


  public static SqlSelectStream of(SqlDataPath jdbcDataPath) throws SelectException {
    return new SqlSelectStream(jdbcDataPath);
  }


  @Override
  public boolean next() {
    try {
      return resultSet.next();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    try {
      resultSet.close();
      this.statement.close();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getString(int columnIndex) {
    try {
      return resultSet.getString(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void beforeFirst() {
    try {
      if (resultSet.getType() == ResultSet.TYPE_FORWARD_ONLY) {
        resultSet.close();
        resultSet = getResultSet();
      } else {
        resultSet.beforeFirst();
      }
    } catch (SQLException | SelectException e) {
      throw new RuntimeException(e);
    }
  }


  private ResultSet getResultSet() throws SelectException {

    if (resultSet != null) {
      return resultSet;
    }

    // Just for debugging purpose, in order to see the query that created this stream
    String query;
    if (this.jdbcDataPath.getMediaType() == SqlMediaType.SCRIPT) {
      query = this.jdbcDataPath.getQuery();
    } else {
      //noinspection SqlDialectInspection
      query = "select * from " + jdbcDataPath.getConnection().getDataSystem().createFromClause(jdbcDataPath);
    }

    try {
      /**
       * The statement is not in a try statement
       * to not close it immediately.
       * Sqlite can not against
       */
      statement = jdbcDataStore.getCurrentConnection().createStatement();
      this.resultSet = statement.executeQuery(query);
    } catch (SQLException e) {
      String message = "An error has occurred executing the query." + Strings.EOL + "Error Message: " + e.getMessage() + Strings.EOL + Strings.EOL + "Query: " + Strings.EOL + query;
      throw new SelectException(message, e);
    }
    return resultSet;

  }


  @Override
  public long getRow() {
    try {
      return resultSet.getRow();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Object getObject(int columnIndex) {
    try {
      return resultSet.getObject(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   *
   */
  @Override
  public SqlRelationDef getRuntimeRelationDef() {
    try {
      SqlRelationDef sqlRelationDef = this.jdbcDataPath.getOrCreateRelationDef();
      if (sqlRelationDef == null) {
        sqlRelationDef = this.jdbcDataPath.getOrCreateRelationDef();
      }
      if (sqlRelationDef.getColumnsSize() == 0) {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
          int columnType = resultSetMetaData.getColumnType(i);
          String columnName = resultSetMetaData.getColumnName(i);
          /*
           * Be sure that we didn't get two column
           * with the same name
           *
           * This can be the case for instance when
           * only the name of a function is returned
           *
           * Postgres do it.
           * For instance:
           *   * avg(col1), avg(col2)
           * will return two columns
           *   * avg, avg
           *
           */
          if (sqlRelationDef.hasColumn(columnName)) {
            columnName = columnName + i;
          }
          sqlRelationDef.addColumn(
            columnName,
            columnType,
            resultSetMetaData.getPrecision(i),
            resultSetMetaData.getScale(i));
        }
      }
      return sqlRelationDef;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Double getDouble(int columnIndex) {
    try {
      return resultSet.getDouble(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Clob getClob(int columnIndex) {
    try {
      return resultSet.getClob(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Retrieves and removes the head of this data path, or returns null if this queue is empty.
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<?> getObjects() {
    return
      jdbcDataPath.getOrCreateRelationDef()
        .getColumnDefs()
        .stream()
        .map(c -> getObject(c.getColumnPosition()))
        .collect(Collectors.toList());
  }

  @Override
  public Integer getInteger(int columnIndex) {
    try {
      return resultSet.getInt(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object getObject(String columnName) {
    try {
      return resultSet.getObject(columnName);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("DuplicatedCode")
  @Override
  public <T> T getObject(int index, Class<T> clazz) {
    Object object;
    try {
      object = resultSet.getObject(index);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    try {
      // Our casting in case the driver has any error
      // such as the sqlite driver that returns false for empty string
      return this.getDataPath().getConnection().getObject(object, clazz);
    } catch (CastException e) {
      // Casting by the driver for weird objects
      try {
        return resultSet.getObject(index, clazz);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    }

  }

  @SuppressWarnings("DuplicatedCode")
  @Override
  public <T> T getObject(String columnName, Class<T> clazz) {
    Object object;
    try {
      object = resultSet.getObject(columnName);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    try {
      // Our casting
      return this.getDataPath().getConnection().getObject(object, clazz);
    } catch (CastException e) {
      // Casting by the driver for weird objects
      try {
        return resultSet.getObject(columnName, clazz);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    }
  }


}
