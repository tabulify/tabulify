package net.bytle.db.stream;

import net.bytle.db.DbLoggers;
import net.bytle.db.model.ISqlRelation;
import net.bytle.db.model.RelationDef;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SqlSelectStream implements SelectStream {

    private final static Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private final ISqlRelation relationDef;
    private ResultSet resultSet;

    public SqlSelectStream(ISqlRelation relationDef) {

        this.relationDef = relationDef;
        this.resultSet = relationDef.getResultSet();

    }

    public static SqlSelectStream get(ISqlRelation relationDef) {
        return new SqlSelectStream(relationDef);
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
    public String getString(int columnIndex) {
        try {
            return resultSet.getString(columnIndex + 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeFirst() {
        try {
            if (resultSet.getType() == ResultSet.TYPE_FORWARD_ONLY) {
                resultSet.close();
                resultSet = relationDef.getResultSet();
            } else {
                resultSet.beforeFirst();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
            return resultSet.getRow();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean previous() {
        try {
            return resultSet.previous();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getObject(int columnIndex) {
        try {
            return resultSet.getObject(columnIndex + 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RelationDef getRelationDef() {
        return relationDef;
    }

    @Override
    public double getDouble(int columnIndex) {
        try {
            return resultSet.getDouble(columnIndex + 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Clob getClob(int columnIndex) {
        try {
            return resultSet.getClob(columnIndex + 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
