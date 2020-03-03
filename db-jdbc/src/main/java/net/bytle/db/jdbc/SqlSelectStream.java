package net.bytle.db.jdbc;

import net.bytle.db.model.RelationDef;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamAbs;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.bytle.db.jdbc.SqlDataPath.QUERY_TYPE;

public class SqlSelectStream extends SelectStreamAbs implements SelectStream {


  private final SqlDataStore jdbcDataStore;

  private SqlDataPath jdbcDataPath;

  // The cursor
  private ResultSet resultSet;

  // Just for debugging purpose, in order to see the query that created this stream
  private String query;

  public SqlSelectStream(SqlDataPath jdbcDataPath) {

    super(jdbcDataPath);
    this.jdbcDataPath = jdbcDataPath;
    this.jdbcDataStore = jdbcDataPath.getDataStore();


  }


  public static SqlSelectStream of(SqlDataPath jdbcDataPath) {
    return new SqlSelectStream(jdbcDataPath);
  }


  @Override
  public boolean next() {
    try {
      return getResultSet().next();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    try {
      getResultSet().close();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getString(int columnIndex) {
    try {
      return getResultSet().getString(columnIndex + 1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void beforeFirst() {
    try {
      if (getResultSet().getType() == ResultSet.TYPE_FORWARD_ONLY) {
        getResultSet().close();
        execute();
      } else {
        getResultSet().beforeFirst();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void execute() {

    getResultSet();

  }

  @Override
  public <T> T getObject(String columnName, Class<T> clazz) {
    try {
      return getResultSet().getObject(columnName, clazz);
    } catch (SQLException e) {
      return this.getDataPath().getDataStore().getObject(getObject(columnName),clazz);
    }
  }

  private ResultSet getResultSet() {
    if (resultSet == null) {
      switch (jdbcDataPath.getType()) {
        case QUERY_TYPE:
          query = jdbcDataPath.getQuery();
          break;
        default:
          query = "select * from " + JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath);
      }

      try {
        this.resultSet = jdbcDataStore.getCurrentConnection().createStatement().executeQuery(query);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    return resultSet;
  }


  @Override
  public long getRow() {
    try {
      return getResultSet().getRow();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Object getObject(int columnIndex) {
    try {
      return getResultSet().getObject(columnIndex + 1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }




  /**
   * @return
   */
  @Override
  public void runtimeDataDef(RelationDef ansiDataDef) {
    try {
      ResultSetMetaData resultSetMetaData = getResultSet().getMetaData();
      for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
        int columnType = resultSetMetaData.getColumnType(i);
        ansiDataDef.addColumn(
          resultSetMetaData.getColumnName(i),
          columnType,
          resultSetMetaData.getPrecision(i),
          resultSetMetaData.getScale(i));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Double getDouble(int columnIndex) {
    try {
      return getResultSet().getDouble(columnIndex + 1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Clob getClob(int columnIndex) {
    try {
      return getResultSet().getClob(columnIndex + 1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Retrieves and removes the head of this data path, or returns null if this queue is empty.
   *
   * @param timeout
   * @param timeUnit
   * @return
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<Object> getObjects() {
    return
      Arrays.stream(jdbcDataPath.getOrCreateDataDef().getColumnDefs())
        .map(c -> getObject(c.getColumnPosition() - 1))
        .collect(Collectors.toList());
  }

  @Override
  public Integer getInteger(int columnIndex) {
    try {
      return getResultSet().getInt(columnIndex + 1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object getObject(String columnName) {
    try {
      return getResultSet().getObject(columnName);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


}
