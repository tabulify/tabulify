package net.bytle.db.jdbc;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlSelectStream extends SelectStreamAbs implements SelectStream {


    private ResultSet resultSet;

    public SqlSelectStream(JdbcDataPath jdbcDataPath) {

        super(jdbcDataPath);

    }

    public static SqlSelectStream of(JdbcDataPath jdbcDataPath) {
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
                initResultSet();
            } else {
                getResultSet().beforeFirst();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initResultSet() {

    }

    private ResultSet getResultSet() {
        if (resultSet==null){
            initResultSet();
        }
        return resultSet;
    }

    @Override
    public boolean first() {
        return false;
    }

    @Override
    public boolean last() {
        return false;
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

    @Override
    public JdbcDataPath getDataPath() {
        return (JdbcDataPath) super.getDataPath();
    }
}
