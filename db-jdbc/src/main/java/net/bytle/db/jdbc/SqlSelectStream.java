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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.bytle.db.jdbc.JdbcDataPath.QUERY_TYPE;
import static net.bytle.db.jdbc.JdbcDataPath.VIEW_TYPE;

public class SqlSelectStream extends SelectStreamAbs implements SelectStream {


    private final JdbcDataSystem jdbcDataSystem;

    private JdbcDataPath jdbcDataPath;

    // The cursor
    private ResultSet resultSet;


    public SqlSelectStream(JdbcDataPath jdbcDataPath) {
        super(jdbcDataPath);
        this.jdbcDataPath = jdbcDataPath;
        this.jdbcDataSystem = jdbcDataPath.getDataSystem();


    }


    public static SqlSelectStream of(JdbcDataPath jdbcDataPath) {
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
                init();
            } else {
                getResultSet().beforeFirst();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() {
        String query;
        switch (jdbcDataPath.getType()) {
            case QUERY_TYPE:
                query = jdbcDataPath.getDataDef().getQuery();
                break;
            default:
                query = "select * from " + JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath);

        }

        try {
            this.resultSet = jdbcDataSystem.getCurrentConnection().createStatement().executeQuery(query);

            if (jdbcDataPath.getType().equals(QUERY_TYPE)) {
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
        if (resultSet == null) {
            init();
        }
        return resultSet;
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
    public Object getObject(int columnIndex) {
        try {
            return getResultSet().getObject(columnIndex + 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TableDef getDataDef() {
        if (resultSet == null) {
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
     * @param timeout
     * @param timeUnit
     * @return
     */
    @Override
    public boolean next(Integer timeout, TimeUnit timeUnit) {
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

    @Override
    public Object getObject(String columnName) {
        try {
            return getResultSet().getObject(columnName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Object> getObjects() {
        return IntStream.of(this.jdbcDataPath.getDataDef().getColumnDefs().size())
                .mapToObj(s->getObject(s))
                .collect(Collectors.toList());
    }
}
