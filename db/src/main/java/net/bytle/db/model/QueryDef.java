package net.bytle.db.model;


import net.bytle.db.database.Database;
import net.bytle.db.engine.Queries;
import net.bytle.db.engine.ResultSets;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A class that contains a query structure definition
 */
public class QueryDef extends RelationDefAbs implements ISqlRelation {


    private final String query;
    private RelationMeta meta;
    private ResultSet resultSet;


    protected QueryDef(Database database, String query, String queryName) {

        if (queryName == null) {
            this.name = query;
        } else {
            this.name = queryName;
        }
        this.query = query;
        this.schema = database.getCurrentSchema();


    }

    protected QueryDef(Database database, String query) {
        this(database, query, null);
    }


    public QueryDef addColumn(String columnName) {

        if (meta == null) {
            initMeta();
        }
        meta.getColumnOf(columnName);
        return this;
    }

    private void initMeta() {

        meta = new RelationMeta(this);
        resultSet = getResultSet();
        ResultSets.addColumns(resultSet, this);

    }


    /**
     * Return the columns by position
     *
     * @return
     */
    public List<ColumnDef> getColumnDefs() {
        if (meta == null) {
            initMeta();
        }
        return meta.getColumnDefs();

    }






    /**
     * @param columnName
     * @return the column or null if not found
     */
    public ColumnDef getColumnDef(String columnName) {

        if (meta == null) {
            initMeta();
        }
        return meta.getColumnDef(columnName);

    }

    /**
     * @param columnName
     * @return the actual column or a new created column object if not found
     */
    public ColumnDef getColumnOf(String columnName) {
        if (meta == null) {
            initMeta();
        }
        return meta.getColumnOf(columnName);

    }



    @Override
    public ColumnDef getColumnDef(Integer columnIndex) {

        if (meta == null) {
            initMeta();
        }
        return meta.getColumnDef(columnIndex);

    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public ResultSet getResultSet() {
        try {
            if (resultSet == null || resultSet.isClosed()) {
                resultSet = Queries.getResultSet(this);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return resultSet;
    }

    @Override
    public String toString() {
        return getName();
    }

    public QueryDef setName(String name) {
        this.name = name;
        return this;
    }
}
