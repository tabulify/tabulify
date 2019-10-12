package net.bytle.db.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

public class JdbcDataSystemSql {

    JdbcDataSystem jdbcDataSystem;

    /**
     * TODO: Move that in a SQL manager
     * The databaseName of a table in a SQL statement
     */
    public String getStatementTableName(JdbcDataPath jdbcDataPath) {

        String identifierQuoteString = "\"";
        try {
            final Connection currentConnection = jdbcDataPath.getDataSystem().getCurrentConnection();
            if (currentConnection!=null) {
                identifierQuoteString = currentConnection.getMetaData().getIdentifierQuoteString();
            }
        } catch (SQLException e) {
            JdbcDataSystemLog.LOGGER_DB_JDBC.warning("The database "+this+" throw an error when retrieving the quoted string identifier."+e.getMessage());
        }
        final String tableName = jdbcDataPath.getName();
        String normativeObjectName = identifierQuoteString+ tableName +identifierQuoteString;
        if (jdbcDataSystem.getSqlDatabase() != null) {
            String objectNameExtension = jdbcDataSystem.getSqlDatabase().getNormativeSchemaObjectName(tableName);
            if (objectNameExtension != null) {
                normativeObjectName = objectNameExtension;
            }
        }
        return normativeObjectName;


    }

    /**
     * Create a merge statement to load data in a table
     * TODO: merge columns can be found at: {@link DatabaseMetaData#getBestRowIdentifier(String, String, String, int, boolean)}
     *
     * @param jdbcDataPath
     * @param mergeColumnPositions
     * @return a merge statement that is used by the loader
     */
    public String getMergeStatement(JdbcDataPath jdbcDataPath, List<Integer> mergeColumnPositions) {

        String sql = "INSERT OR REPLACE INTO " + jdbcDataPath.getName() + "(";

        // Columns
        String columnsName = "TODO";
        // Level 8 syntax
        //        tableDef.getColumnDefs().stream()
        //                .map(ColumnDef::getColumnName)
        //                .collect(Collectors.joining(", "));

        sql += columnsName + ") values (";

        for (int i = 0; i < jdbcDataPath.getDataDef().getColumnDefs().size(); i++) {
            sql += "?";
            if (!(i >= jdbcDataPath.getDataDef().getColumnDefs().size() - 1)) {
                sql += ",";
            }
        }
        sql += ")";

        return sql;

    }

    /**
     * The generation of a SQL must not be inside
     *
     * @return
     */
    public String getQuery(JdbcDataPath dataPath) {
        /**
         * {@link DatabaseMetaData#getIdentifierQuoteString()}
         */
        return "select * from " + getFullyQualifiedSqlName(dataPath);
    }

    /**
     * The fully qualified name is the name with its schema
     * that can be used in SQL Statement
     *
     * @param jdbcDataPath
     * @return
     */
    public String getFullyQualifiedSqlName(JdbcDataPath jdbcDataPath) {


        final String statementTableName = getStatementTableName(jdbcDataPath);

        // No schema functionality (Sqlite has a schema on database level)
        if (jdbcDataPath.getSchema() == null) {
            return statementTableName;
        } else {
            /**
             * Only for catalog
             * {@link DatabaseMetaData#getCatalogSeparator()}
             */
            return jdbcDataPath.getSchema() + "." + statementTableName;
        }


    }


}
