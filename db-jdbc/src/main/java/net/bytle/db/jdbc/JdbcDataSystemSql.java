package net.bytle.db.jdbc;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;

import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * Sql statement are in the class {@link SqlDataSystem} closed to the operations
 * that they supports
 *
 * For example, the truncate statement can be found at {@link SqlDataSystem#truncateStatement(AnsiDataPath)}
 * next to the {@link SqlDataSystem#truncate(DataPath)} operations
 *
 * The SQL function below
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
    public static String getQuotedTableName(AnsiDataPath jdbcDataPath) {


        final SqlDataStore dataStore = jdbcDataPath.getDataStore();
        String identifierQuoteString = dataStore.getIdentifierQuote();
        final String tableName = jdbcDataPath.getName();
        return identifierQuoteString+ tableName +identifierQuoteString;


    }

    public static String getQueryColumnName(ColumnDef columnDef) {
        final AnsiDataPath dataPath = (AnsiDataPath) columnDef.getDataDef().getDataPath();
        String identifier = dataPath.getDataStore().getIdentifierQuote();
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
    public static String getQueryColumnsStatement(AnsiDataPath jdbcDataPath) {
        /**
         * {@link DatabaseMetaData#getIdentifierQuoteString()}
         */
        assert jdbcDataPath.getOrCreateDataDef().getColumnsSize() !=0 : "The table ("+jdbcDataPath+") does not have any columns, a create statement cannot be created";
        return IntStream.range(0,jdbcDataPath.getOrCreateDataDef().getColumnsSize())
                .mapToObj(i->jdbcDataPath.getOrCreateDataDef().getColumnDef(i).getColumnName())
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
    public String getMergeStatement(AnsiDataPath jdbcDataPath, List<Integer> mergeColumnPositions) {

        String sql = "INSERT OR REPLACE INTO " + jdbcDataPath.getName() + "(";

        // Columns
        String columnsName = "TODO";
        // Level 8 syntax
        //        tableDef.getColumnDefs().stream()
        //                .map(ColumnDef::getColumnName)
        //                .collect(Collectors.joining(", "));

        sql += columnsName + ") values (";

        for (int i = 0; i < jdbcDataPath.getOrCreateDataDef().getColumnsSize(); i++) {
            sql += "?";
            if (!(i >= jdbcDataPath.getOrCreateDataDef().getColumnsSize() - 1)) {
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
    public static String getSelectStatement(AnsiDataPath dataPath) {

        /**
         * If it does not work, "select * from " + getFullyQualifiedSqlName(dataPath); ?
         */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SELECT ");
        stringBuilder.append(JdbcDataSystemSql.getQueryColumnsStatement(dataPath));
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

        AnsiDataPath jdbcDataPath = (AnsiDataPath) dataPath;

        final String statementTableName = getQuotedTableName(jdbcDataPath);

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






}
