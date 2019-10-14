package net.bytle.db.jdbc;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class SqlSelectStream extends SelectStreamAbs implements SelectStream {


    // A SqlSelectStream comes from a query or
    // from a dataPath representing a data object
    private String query;
    private JdbcDataPath jdbcDataPath;

    // The cursor
    private ResultSet resultSet;

    // The DataDef (It comes from the data path or from the result set of a query)
    private TableDef dataDef;

    public SqlSelectStream(JdbcDataPath jdbcDataPath) {

        this.jdbcDataPath = jdbcDataPath;
        this.dataDef = jdbcDataPath.getDataDef();

    }

    public SqlSelectStream(JdbcDataPath jdbcDataPath, String query) {
        if (!jdbcDataPath.getDataSystem().isContainer(jdbcDataPath)){
            throw new RuntimeException("The data path of a query cannot be a table");
        }
        this.jdbcDataPath = jdbcDataPath;
        this.query = query;
    }

    public static SqlSelectStream of(JdbcDataPath jdbcDataPath) {
        return new SqlSelectStream(jdbcDataPath);
    }

    public static SqlSelectStream of(JdbcDataPath jdbcDataPath, String query) {
        return new SqlSelectStream(jdbcDataPath, query);
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
            this.resultSet = jdbcDataPath.getDataSystem().getCurrentConnection().createStatement().executeQuery(query);

            if (this.dataDef ==null) {
                ResultSetMetaData resultSetMetaData = this.resultSet.getMetaData();
                this.dataDef = TableDef.of(jdbcDataPath);
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
        if (this.dataDef==null){
            init();
        }
        return this.dataDef;
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


}
