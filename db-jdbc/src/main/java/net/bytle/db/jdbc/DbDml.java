package net.bytle.db.jdbc;

import net.bytle.db.database.SqlDataTypesManager;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;

import java.util.List;


public class DbDml {


    /**
     * Return an insert statement where the AutoIncrement Column are not added
     *
     * @param relationDef
     * @return
     */
    public static String getParameterizedInsertStatement(RelationDef relationDef) {

        if (relationDef.getColumnsSize() == 0) {
            throw new RuntimeException("The table (" + relationDef.getDataPath() + ") has no columns. We can not create an insert statement.");
        }
        String insertStatement = "INSERT INTO " + JdbcDataSystemSql.getFullyQualifiedSqlName(relationDef.getDataPath()) + " (";
        String insertStatementBindVariable = "";

        // Loop to create the statement
        for (ColumnDef columnDef : relationDef.getColumnDefs()) {
            if (!columnDef.getIsAutoincrement().equals("YES")) {
                insertStatement += "\"" + columnDef.getColumnName() + "\", ";
                insertStatementBindVariable += "?, ";
            }
        }

        // Suppress the last comma
        insertStatement = insertStatement.substring(0, insertStatement.length() - 2);
        insertStatementBindVariable = insertStatementBindVariable.substring(0, insertStatementBindVariable.length() - 2);

        insertStatement += ") VALUES (" + insertStatementBindVariable + ")";

        return insertStatement;

    }

    /**
     * Return a parameterized insert statement again the tableDef from the resultSetMetdata
     *
     * @param target
     * @param source
     * @return
     */
    public static String getParameterizedInsertStatement(RelationDef target, RelationDef source) {


        return getInsertStatement(target, source, null);
    }

    /**
     * Return a insert statement again the tableDef from the resultSetMetdata
     *
     * @param target - the target table
     * @param source - the source relation
     * @param values - the value to insert (If values is null, it will return a parameterized statement)
     * @return
     */
    public static String getInsertStatement(RelationDef source, RelationDef target, List<Object> values) {

        final SqlDataPath dataPath = (SqlDataPath) target.getDataPath();
        String insertStatement = "INSERT INTO " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath) + " (";
        String insertStatementBindVariable = "";

        try {
            for (int i = 0; i < source.getColumnsSize(); i++) {
                String colName = source.getColumnDef(i).getColumnName();
                ColumnDef columnDef = target.getColumnDef(colName);
                if (!columnDef.getIsAutoincrement()) {
                    String fieldQuote = "\"";
                    if (dataPath.getDataStore().getCurrentConnection().getMetaData().getDatabaseProductName().equals(SqlDataStore.DB_HIVE)) {
                        fieldQuote = "`";
                    }
                    insertStatement += fieldQuote + columnDef.getColumnName() + fieldQuote + ", ";
                    if (values == null) {
                        insertStatementBindVariable += "?, ";
                    } else {
                        Object value = values.get(i);
                        if (value == null) {
                            insertStatementBindVariable += "null, ";
                        } else {
                            if (SqlDataTypesManager.isNumeric(columnDef.getDataType().getTypeCode())) {
                                insertStatementBindVariable += value.toString() + ", ";
                            } else {
                                insertStatementBindVariable += "'" + value.toString() + "', ";
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Suppress the last comma
        insertStatement = insertStatement.substring(0, insertStatement.length() - 2);
        insertStatementBindVariable = insertStatementBindVariable.substring(0, insertStatementBindVariable.length() - 2);

        insertStatement += ") VALUES (" + insertStatementBindVariable + ")";

        return insertStatement;

    }

    /**
     *
     * @param source
     * @param target
     * @return an insert into statement
     */
    public static String getInsertIntoStatement(SqlDataPath source, SqlDataPath target) {

        StringBuilder insertIntoBuilder = new StringBuilder();
        insertIntoBuilder.append("INSERT INTO " + JdbcDataSystemSql.getFullyQualifiedSqlName(target) + " (");
        insertIntoBuilder.append(JdbcDataSystemSql.getQueryColumnsStatement(target));
        insertIntoBuilder.append(") ");
        if (source.getType().equals(SqlDataPath.Type.QUERY.toString())){

            insertIntoBuilder.append(source.getQuery());

        } else {

            JdbcDataSystemSql.getSelectStatement(source);
        }
        return insertIntoBuilder.toString();

    }
}
