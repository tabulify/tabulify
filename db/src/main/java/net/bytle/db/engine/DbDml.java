package net.bytle.db.engine;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;

import java.util.List;

import static net.bytle.db.database.Database.DB_HIVE;
import static net.bytle.db.engine.DataTypes.isNumeric;

public class DbDml {



    /**
     * Return an insert statement where the AutoIncrement Column are not added
     *
     * @param tableDef
     * @return
     */
    public static String getParameterizedInsertStatement(RelationDef tableDef) {

        if (tableDef.getColumnDefs().size() == 0) {
            throw new RuntimeException("The table (" + tableDef.getFullyQualifiedName() + ") has no columns. We can not create an insert statement.");
        }
        String insertStatement = "INSERT INTO " + tableDef.getFullyQualifiedName() + " (";
        String insertStatementBindVariable = "";

        // Loop to create the statement
        for (ColumnDef columnDef : tableDef.getColumnDefs()) {
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
    public static String getParameterizedInsertStatement(TableDef target, RelationDef source) {


        return getInsertStatement(target,source,null);
    }

    /**
     * Return a insert statement again the tableDef from the resultSetMetdata
     *
     * @param target - the target table
     * @param source - the source relation
     * @param values - the value to insert (If values is null, it will return a parameterized statement)
     * @return
     *
     */
    public static String getInsertStatement(TableDef target, RelationDef source, List<Object> values) {

        String insertStatement = "INSERT INTO " + target.getFullyQualifiedName() + " (";
        String insertStatementBindVariable = "";

        try {
            for (int i = 0; i < source.getColumnDefs().size(); i++) {
                String colName = source.getColumnDef(i).getColumnName();
                ColumnDef columnDef = target.getColumnOf(colName);
                if (!columnDef.getIsAutoincrement().equals("YES")) {
                    String fieldQuote = "\"";
                    if (target.getDatabase().getDatabaseProductName().equals(DB_HIVE)){
                        fieldQuote = "`";
                    }
                    insertStatement += fieldQuote + columnDef.getColumnName() + fieldQuote + ", ";
                    if (values==null) {
                        insertStatementBindVariable += "?, ";
                    } else {
                        Object value = values.get(i);
                        if (value == null){
                            insertStatementBindVariable += "null, ";
                        } else {
                            if (isNumeric(columnDef.getDataType().getTypeCode())) {
                                insertStatementBindVariable += value.toString()+", ";
                            } else {
                                insertStatementBindVariable += "'"+value.toString()+"', ";
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

}
