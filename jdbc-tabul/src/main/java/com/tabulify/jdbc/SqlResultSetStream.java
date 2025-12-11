package com.tabulify.jdbc;

import com.tabulify.fs.sql.SqlStatement;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import com.tabulify.type.KeyNormalizer;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SqlResultSetStream extends SelectStreamAbs implements SelectStream {


  private final SqlRequest jdbcDataPath;
  private final SqlStatement sqlStatement;

  // The cursor
  private ResultSet resultSet;


  public SqlResultSetStream(SqlRequest jdbcDataPath, ResultSet resultSet, SqlStatement sqlStatement) {

    super(jdbcDataPath);
    this.jdbcDataPath = jdbcDataPath;

    this.resultSet = resultSet;
    this.sqlStatement = sqlStatement;


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
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isClosed() {
    try {
      return this.resultSet.isClosed();
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
        SqlResultSetStream sqlResultSetStream = (SqlResultSetStream) SqlRequestExecution.getSqlStreamResultSet(this.jdbcDataPath, this.sqlStatement);

        resultSet = sqlResultSetStream.getResultSet();
      } else {
        resultSet.beforeFirst();
      }
    } catch (SQLException | SelectException e) {
      throw new RuntimeException(e);
    }
  }

  private ResultSet getResultSet() {
    return this.resultSet;
  }

  @Override
  public long getRecordId() {
    try {
      return resultSet.getRow();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Object getObject(ColumnDef columnDef) {
    try {
      return resultSet.getObject(columnDef.getColumnPosition());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   *
   */
  @Override
  public SqlDataPathRelationDef getRuntimeRelationDef() {

    SqlDataPathRelationDef sqlDataPathRelationDef = this.jdbcDataPath.getRelationDef();
    if (sqlDataPathRelationDef == null) {
      sqlDataPathRelationDef = this.jdbcDataPath.createEmptyRelationDef();
    }
    if (sqlDataPathRelationDef.getColumnsSize() == 0) {
      ResultSetMetaData resultSetMetaData;
      try {
        resultSetMetaData = resultSet.getMetaData();
      } catch (SQLException e) {
        throw new RuntimeException("Error while trying to retrieve the runtime metadata of the data resource (" + this.getDataPath() + "). Error: " + e.getMessage(), e);
      }
      mergeResultSetMetadata(sqlDataPathRelationDef, resultSetMetaData);
    }
    return sqlDataPathRelationDef;

  }

  public static void mergeResultSetMetadata(SqlDataPathRelationDef sqlDataPathRelationDef, ResultSetMetaData resultSetMetaData) {
    try {
      for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
        int columnType = resultSetMetaData.getColumnType(i);
        KeyNormalizer columnTypeName = KeyNormalizer.createSafe(resultSetMetaData.getColumnTypeName(i));
        SqlDataType<?> type = sqlDataPathRelationDef.getDataPath().getConnection().getSqlDataType(columnTypeName, columnType);
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
        if (sqlDataPathRelationDef.hasColumn(columnName)) {
          columnName = columnName + i;
        }
        sqlDataPathRelationDef.addColumn(
          columnName,
          type,
          resultSetMetaData.getPrecision(i),
          resultSetMetaData.getScale(i),
          null,
          null
        );
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error while traversing the runtime metadata of the data resource (" + sqlDataPathRelationDef.getDataPath() + "). Error: " + e.getMessage(), e);
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
    return getRuntimeRelationDef()
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

  @Override
  public SqlDataPath getDataPath() {
    return this.jdbcDataPath;
  }

  @Override
  public <T> T getObject(ColumnDef<?> columnDef, Class<T> clazz) {

    return this.getDataPath().getConnection().getObjectFromResulSet(resultSet, columnDef, clazz);
  }


}
