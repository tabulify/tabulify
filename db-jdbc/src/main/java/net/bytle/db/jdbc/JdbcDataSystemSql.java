package net.bytle.db.jdbc;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     *
     * The databaseName of a table in a SQL statement
     */
    public static String getStatementTableName(JdbcDataPath jdbcDataPath) {


        final AnsiDataStore dataStore = jdbcDataPath.getDataStore();
        String identifierQuoteString = DbSql.getIdentifierQuote(dataStore);
        final String tableName = jdbcDataPath.getName();
        return identifierQuoteString+ tableName +identifierQuoteString;


    }

    public static String getFullyQualifiedSqlName(ColumnDef columnDef) {
        final JdbcDataPath dataPath = (JdbcDataPath) columnDef.getDataDef().getDataPath();
        String identifier = DbSql.getIdentifierQuote(dataPath.getDataStore());
        return getFullyQualifiedSqlName(dataPath)+"."+identifier+columnDef.getColumnName()+identifier;
    }

    /**
     *
     * @param jdbcDataPath
     * @return a list of column name separated by a comma
     *
     * Example:
     * col1, col2, col3
     */
    public static String getColumnsStatement(JdbcDataPath jdbcDataPath) {
        /**
         * {@link DatabaseMetaData#getIdentifierQuoteString()}
         */
        assert jdbcDataPath.getDataDef().getColumnsSize() !=0 : "The table ("+jdbcDataPath+") does not have any columns, a create statement cannot be created";
        return IntStream.range(0,jdbcDataPath.getDataDef().getColumnsSize())
                .mapToObj(i->jdbcDataPath.getDataDef().getColumnDef(i).getColumnName())
                .collect(Collectors.joining(", "));
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

        for (int i = 0; i < jdbcDataPath.getDataDef().getColumnsSize(); i++) {
            sql += "?";
            if (!(i >= jdbcDataPath.getDataDef().getColumnsSize() - 1)) {
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
    public static String getSelectStatement(JdbcDataPath dataPath) {

        /**
         * If it does not work, "select * from " + getFullyQualifiedSqlName(dataPath); ?
         */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SELECT ");
        stringBuilder.append(JdbcDataSystemSql.getColumnsStatement(dataPath));
        stringBuilder.append(" FROM ");
        stringBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));

        return stringBuilder.toString();
    }

    /**
     * The fully qualified name is the name with its schema
     * that can be used in SQL Statement
     *
     * @param dataPath
     * @return
     */
    public static String getFullyQualifiedSqlName(DataPath dataPath) {

        JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;

        final String statementTableName = getStatementTableName(jdbcDataPath);

        // No schema functionality (Sqlite has a schema on database level)
        if (jdbcDataPath.getSchema() == null) {
            return statementTableName;
        } else {
            /**
             * Only for catalog
             * {@link DatabaseMetaData#getCatalogSeparator()}
             */
            if (jdbcDataPath.getSchema().getName().equals("")){
              return statementTableName;
            } else {
              return jdbcDataPath.getSchema().getName() + "." + statementTableName;
            }
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
          ResultSet resultSet = jdbcDataPath.getDataStore().getCurrentConnection().createStatement().executeQuery(statementString);
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
