package net.bytle.db.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * Schema implementation
 *
 * See also:
 * {@link DatabaseMetaData#supportsSchemasInDataManipulation()}
 * {@link DatabaseMetaData#supportsSchemasInIndexDefinitions()}
 * {@link DatabaseMetaData#supportsSchemasInPrivilegeDefinitions()}
 * {@link DatabaseMetaData#supportsSchemasInProcedureCalls()}
 * {@link DatabaseMetaData#supportsSchemasInTableDefinitions()}
 *
 * {@link DatabaseMetaData#supportsCatalogsInDataManipulation()}
 * {@link DatabaseMetaData#supportsCatalogsInIndexDefinitions()}
 * {@link DatabaseMetaData#supportsCatalogsInPrivilegeDefinitions()}
 * {@link DatabaseMetaData#supportsCatalogsInProcedureCalls()}
 * {@link DatabaseMetaData#supportsCatalogsInTableDefinitions()}
 *
 */

public class JdbcDataSystemSql {


    /**
     * TODO: Move that in a SQL manager
     * The databaseName of a table in a SQL statement
     */
    public static String getStatementTableName(JdbcDataPath jdbcDataPath) {

        String identifierQuoteString = "\"";
        final JdbcDataSystem dataSystem = jdbcDataPath.getDataSystem();
        try {
            final Connection currentConnection = dataSystem.getCurrentConnection();
            if (currentConnection!=null) {
                identifierQuoteString = currentConnection.getMetaData().getIdentifierQuoteString();
            }
        } catch (SQLException e) {
            JdbcDataSystemLog.LOGGER_DB_JDBC.warning("The database "+dataSystem.getDatabase()+" throw an error when retrieving the quoted string identifier."+e.getMessage());
        }
        final String tableName = jdbcDataPath.getName();
        String normativeObjectName = identifierQuoteString+ tableName +identifierQuoteString;
        if (dataSystem.getSqlDatabase() != null) {
            String objectNameExtension = dataSystem.getSqlDatabase().getNormativeSchemaObjectName(tableName);
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
    public static String getFullyQualifiedSqlName(JdbcDataPath jdbcDataPath) {


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

    /**
     * Return the number of rows
     *
     * @param jdbcDataPath - A tableDef
     * @return - the number of rows for this table
     */
    public static Integer getSize(JdbcDataPath jdbcDataPath) {



        Integer returnValue = 0;
        String statementString = "select count(1) from " + getFullyQualifiedSqlName(jdbcDataPath);

        try (
                ResultSet resultSet = jdbcDataPath.getDataSystem().getCurrentConnection().createStatement().executeQuery(statementString);
        ) {
            while (resultSet.next()) {
                returnValue += resultSet.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println(statementString);
            throw new RuntimeException(e);
        }
        return returnValue;

    }




}
