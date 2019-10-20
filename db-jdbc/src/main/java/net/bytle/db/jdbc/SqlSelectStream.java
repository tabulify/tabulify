package net.bytle.db.jdbc;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SqlSelectStream extends SelectStreamAbs implements SelectStream {


    private final JdbcDataSystem jdbcDataSystem;
    // A SqlSelectStream comes from a query or
    // from a dataPath representing a data object
    private String query;
    private JdbcDataPath jdbcDataPath;

    // The cursor
    private ResultSet resultSet;


    public SqlSelectStream(JdbcDataPath jdbcDataPath) {

        this.jdbcDataPath = jdbcDataPath;
        this.jdbcDataSystem = jdbcDataPath.getDataSystem();


    }

    public SqlSelectStream(JdbcDataSystem jdbcDataSystem, String query) {

        this.jdbcDataSystem = jdbcDataSystem;
        this.jdbcDataPath = jdbcDataSystem.getDataPath("");
        this.query = query;
    }

    public static SqlSelectStream of(JdbcDataPath jdbcDataPath) {
        return new SqlSelectStream(jdbcDataPath);
    }

    public static SqlSelectStream of(JdbcDataSystem jdbcDataSystem, String query) {
        return new SqlSelectStream(jdbcDataSystem, query);
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
                init();
            } else {
                getResultSet().beforeFirst();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() {
        String query = this.query;
        if (query==null){
            query = "select * from "+JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath);
        }
        try {
            this.resultSet = jdbcDataSystem.getCurrentConnection().createStatement().executeQuery(query);

            if (this.query!=null) {
                ResultSetMetaData resultSetMetaData = this.resultSet.getMetaData();
                TableDef dataDef = jdbcDataPath.getDataDef();
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    dataDef.addColumn(
                            resultSetMetaData.getColumnName(i),
                            resultSetMetaData.getColumnType(i),
                            resultSetMetaData.getPrecision(i),
                            resultSetMetaData.getScale(i));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ResultSet getResultSet() {
        if (resultSet==null){
            init();
        }
        return resultSet;
    }

    @Override
    public boolean first() {
        try {
            return getResultSet().first();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean last() {
        try {
            return getResultSet().last();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRow() {
        try {
            return getResultSet().getRow();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean previous() {
        try {
            return getResultSet().previous();
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

    @Override
    public TableDef getDataDef() {
        if (resultSet==null){
            init();
        }
        return this.jdbcDataPath.getDataDef();
    }


    @Override
    public double getDouble(int columnIndex) {
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
     * @param i
     * @param timeUnit
     * @return
     */
    @Override
    public List<Object> poll(int i, TimeUnit timeUnit) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Integer getInteger(int columnIndex) {
        try {
            return getResultSet().getInt(columnIndex + 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
