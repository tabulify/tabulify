package net.bytle.db.engine;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.Database;
import net.bytle.db.model.*;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class DbDdl {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Return create statements (inclusive primary key, foreign key and unique key)
     *
     * @param tableDef The table Name in the create statement
     */
    public static List<String> getCreateTableStatements(TableDef tableDef) {

        return getCreateTableStatements(tableDef, tableDef.getSchema(), tableDef.getName());
    }

    /**
     * Return create statements (inclusive primary key, foreign key and unique key)
     *
     * @param tableDef The table Name in the create statement
     */
    public static List<String> getCreateTableStatements(TableDef tableDef, SchemaDef schemaDef) {

        return getCreateTableStatements(tableDef, schemaDef, tableDef.getName());
    }

    /**
     * Return create statements (inclusive primary key, foreign key and unique key)
     *
     * @param tableDef The table Name in the create statement
     * @param schemaDef The target schema
     */
    public static List<String> getCreateTableStatements(TableDef tableDef, SchemaDef schemaDef, String name) {


        List<String> statements = new ArrayList<>();
        Database targetDatabase = schemaDef.getDatabase();

        // If the databaseDefault implements its own logic, we return it.
        try {
            statements = targetDatabase.getSqlDatabase().getCreateTableStatements(tableDef, name);
            if (statements != null) {
                if (statements.size() == 0) {
                    LOGGER.warning("The database extension " + targetDatabase.getSqlDatabase() + " returns 0 statements.");
                } else {
                    return statements;
                }
            } else {

                statements = new ArrayList<>();

            }


        } catch (Exception e) {

            LOGGER.severe("The getCreateTableStatements of the database provider " + tableDef.getDatabase().getDatabaseProductName() + " returns an error.");
            throw new RuntimeException(e);

        }


        // The database extension doesn't implements its own logic, we create then a standard SQL

        StringBuilder createTableStatement = new StringBuilder()
                .append("create table ");
        final String schemaName = schemaDef.getName();
        if (schemaName != null) {
            createTableStatement.append(schemaName).append(".");
        }
        createTableStatement
                .append(name)
                .append(" (\n")
                .append(getCreateTableStatementColumnsDefinition(tableDef.getColumnDefs(), schemaDef))
                .append(" )\n");
        statements.add(createTableStatement.toString());

        // Primary Key
        final PrimaryKeyDef primaryKey = tableDef.getPrimaryKey();
        if (primaryKey!=null) {
            if (primaryKey.getColumns().size() != 0) {
                String createPrimaryKeyStatement = getAlterTablePrimaryKeyStatement(primaryKey, schemaDef);
                if (createPrimaryKeyStatement != null) {
                    statements.add(createPrimaryKeyStatement);
                }
            }
        }

        // Foreign key
        for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
            String createForeignKeyStatement = getAlterTableForeignKeyStatement(foreignKeyDef, schemaDef);
            if (createForeignKeyStatement != null) {
                statements.add(createForeignKeyStatement);
            }
        }

        // Unique key
        for (UniqueKeyDef uniqueKeyDef : tableDef.getUniqueKeys()) {
            String createUniqueKeyStatement = getAlterTableUniqueKeyStatement(uniqueKeyDef, schemaDef);
            statements.add(createUniqueKeyStatement);
        }

        return statements;

    }


    /**
     * @param columnDefs : The result set meta contains the table columns structures
     * @param schemaDef  : The target schema
     * @return the column string part of a create statement
     */
    public static String getCreateTableStatementColumnsDefinition(List<ColumnDef> columnDefs, SchemaDef schemaDef) {


        StringBuilder statementColumnPart = new StringBuilder();
        for (int i = 0; i < columnDefs.size(); i++) {

            try {

                ColumnDef columnDef = columnDefs.get(i);
                // Add it to the columns statement
                statementColumnPart.append(getColumnStatementForCreateTable(columnDef, schemaDef));

            } catch (Exception e) {

                throw new RuntimeException(e + "\nException: The Column Statement build until now is:\n" + statementColumnPart, e);

            }

            // Is it the end ...
            if (i != columnDefs.size() - 1) {
                statementColumnPart.append(",\n");
            } else {
                statementColumnPart.append("\n");
            }

        }

        return statementColumnPart.toString();

    }

    /**
     * A blank statement in the form "columnName datatype(scale, precision)"
     * The constraint such as NOT NULL unique may change between database
     * Example Sqlite has the primary key statement before NOT NULL
     *
     * @param columnDef : The source column
     * @param schemaDef : The target schema
     * @return
     */
    public static String getColumnStatementForCreateTable(ColumnDef columnDef, SchemaDef schemaDef) {

        DataType targetDataType = schemaDef.getDatabase().getDataType(columnDef.getDataType().getTypeCode());

        // Always passed to create the statement
        Integer precision = columnDef.getPrecision();
        if (precision == null) {
            precision = targetDataType.getMaxPrecision();
        }
        Integer scale = columnDef.getScale();
        if (scale == null) {
            scale = targetDataType.getMaximumScale();
        }

        String dataTypeCreateStatement = null;

        DataTypeDatabase dataTypeDatabase = targetDataType.getDataTypeDatabase();
        if (dataTypeDatabase != null) {
            dataTypeCreateStatement = dataTypeDatabase.getCreateStatement(precision, scale);
        }

        if (dataTypeCreateStatement == null) {
            if (targetDataType.getTypeCode() == Types.DATE || targetDataType.getTypeCode() == Types.TIME) {

                dataTypeCreateStatement = targetDataType.getTypeName();


            } else {


                if (targetDataType != null) {
                    dataTypeCreateStatement = getCreateDataTypeStatement(targetDataType, precision, scale);
                } else {
                    String columnTypeName;
                    try {
                        columnTypeName = columnDef.getDataType().getTypeName();
                        dataTypeCreateStatement = getCreateDataTypeStatement(columnTypeName, precision, scale);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        // NOT NULL
        String notNullStatement = "";
        if (columnDef.getNullable() == DatabaseMetaData.columnNoNulls) {
            // Hack because hive is read only, it does not support Not Null
            if (!schemaDef.getDatabase().getDatabaseProductName().equals(Database.DB_HIVE)) {
                notNullStatement = " NOT NULL";
            }
        }

        // Hack for Hive
        String encloseString = "\"";
        if (schemaDef.getDatabase().getDatabaseProductName().equals(Database.DB_HIVE)) {
            encloseString = "`";
        }

        // Number as columnName is not possible in Oracle
        // Just with two double quote
        return encloseString + columnDef.getColumnName() + encloseString + " " + dataTypeCreateStatement + notNullStatement;
    }


    /**
     * columnTypeName is needed as we can see inconsistency between
     * the column type name and the column type int
     * Ex: Oracle dataTypeInfo: 93 -> Timestamp
     * resultSet: 93 -> Date
     * This function returns always the resultSet value, the parameter value
     * No no ! ... in a source target, the columnType are not the same
     *
     * @return the create statement
     */
    public static String getCreateDataTypeStatement(DataType dataType, Integer precision, Integer scale) {


        if (dataType.getDataTypeDriver() != null) {

            if (dataType.getTypeCode() == Types.VARCHAR) {
                if (precision == 0) {
                    precision = dataType.getMaxPrecision();
                }
            }


            final Integer maxPrecision = dataType.getMaxPrecision();
            if (precision > maxPrecision && maxPrecision !=0) {
                precision = dataType.getMaxPrecision();
            } else if (precision < 0) {
                precision = 0;
            }

            if (scale != 0) {
                if (scale > dataType.getMaximumScale()) {
                    scale = dataType.getMaximumScale();
                } else if (scale < dataType.getMinimumScale()) {
                    scale = dataType.getMinimumScale();
                }
            }

            return getCreateDataTypeStatement(dataType.getTypeName(), precision, scale);

        } else {

            return getCreateDataTypeStatement(dataType.getTypeName(), precision, scale);

        }

    }

    static String getCreateDataTypeStatement(String columnTypeName, Integer precision, Integer scale) {

        String dataTypeCreateStatement = columnTypeName;
        if (precision > 0) {
            dataTypeCreateStatement += "(" + precision;
            if (scale != 0) {
                dataTypeCreateStatement += "," + scale;
            }
            dataTypeCreateStatement += ")";
        }
        return dataTypeCreateStatement;

    }

    /**
     * @param uniqueKeyDef - The source unique key def
     * @param schemaDef    - The target schema
     * @return an alter table unique statement
     */
    public static String getAlterTableUniqueKeyStatement(UniqueKeyDef uniqueKeyDef, SchemaDef schemaDef) {

        String statement = "ALTER TABLE " + schemaDef.getName() + "." + uniqueKeyDef.getTableDef().getName() + " ADD ";

        // The serie of columns definitions (col1, col2,...)
        final List<ColumnDef> columns = uniqueKeyDef.getColumns();
        List<String> columnNames = new ArrayList<>();
        for (ColumnDef columnDef : columns) {
            columnNames.add(columnDef.getColumnName());
        }
        final String columnDefStatement = String.join(",", columnNames.toArray(new String[columnNames.size()]));

        // The final statement that presence of the name
        final String name = uniqueKeyDef.getName();
        if (name == null) {
            statement = statement + "UNIQUE (" + columnDefStatement + ") ";
        } else {
            statement = statement + "CONSTRAINT " + name + " UNIQUE (" + columnDefStatement + ") ";
        }

        return statement;

    }

    /**
     * @param foreignKeyDef   - The source foreign key
     * @param targetSchemaDef - The target schema
     * @return a alter table foreign key statement
     */
    public static String getAlterTableForeignKeyStatement(ForeignKeyDef foreignKeyDef, SchemaDef targetSchemaDef) {

        // Constraint are supported from 2.1
        // https://issues.apache.org/jira/browse/HIVE-13290
        if (targetSchemaDef.getDatabase().getDatabaseProductName().equals(Database.DB_HIVE)) {
            if (targetSchemaDef.getDatabase().getDatabaseMajorVersion() < 2) {
                return null;
            } else {
                if (targetSchemaDef.getDatabase().getDatabaseMinorVersion() < 1) {
                    return null;
                }
            }
        }
        StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
        if (targetSchemaDef.getName() != null) {
            statement
                    .append(targetSchemaDef.getName())
                    .append(".");
        }
        statement.append(foreignKeyDef.getTableDef().getName())
                .append(" ADD ")
                .append("CONSTRAINT ")
                .append(foreignKeyDef.getName())
                .append(" FOREIGN KEY (");
        final List<ColumnDef> nativeColumns = foreignKeyDef.getChildColumns();
        for (int i = 0; i < nativeColumns.size(); i++) {
            statement.append(nativeColumns.get(i).getColumnName());
            if (i!=nativeColumns.size()){
                statement.append(", ");
            }
        }


        statement
                .append("REFERENCES ")
                .append(targetSchemaDef.getName())
                .append(".")
                .append(foreignKeyDef.getForeignPrimaryKey().getTableDef().getName())
                .append(" (");
        List<ColumnDef> foreignColumns = foreignKeyDef.getForeignPrimaryKey().getColumns();
        for (int i=0;i < foreignColumns.size();i++) {
            statement.append(foreignColumns.get(i).getColumnName());
            if (i!=foreignColumns.size()){
                statement.append(", ");
            }
        }
        statement.append(")");

        return statement.toString();


    }

    public static String getAlterTablePrimaryKeyStatement(PrimaryKeyDef primaryKeyDef, SchemaDef schemaDef) {

        List<ColumnDef> columns = primaryKeyDef.getColumns();
        int size = columns.size();
        if (size == 0) {
            return null;
        }

        // TODO: Move to Hive
        // Constraint are supported from 2.1
        // https://issues.apache.org/jira/browse/HIVE-13290
        if (schemaDef.getDatabase().getDatabaseProductName().equals(Database.DB_HIVE)) {
            if (schemaDef.getDatabase().getDatabaseMajorVersion() < 2) {
                return null;
            } else {
                if (schemaDef.getDatabase().getDatabaseMinorVersion() < 1) {
                    return null;
                }
            }
        }

        StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
        if (schemaDef.getName() != null) {
            statement
                    .append(schemaDef.getName())
                    .append(".");
        }
        statement
                .append(primaryKeyDef.getTableDef().getName())
                .append(" ADD ");
        if (primaryKeyDef.getName() != null) {
            statement
                    .append("CONSTRAINT ")
                    .append(primaryKeyDef.getName())
                    .append(" ");
        }
        List<String> columnNames = new ArrayList<>();
        for (ColumnDef columnDef : columns) {
            columnNames.add(columnDef.getColumnName());
        }
        statement
                .append("PRIMARY KEY  (")
                .append(String.join(", ", columnNames))
                .append(")");

        return statement.toString();
    }


    public static void truncateTable(TableDef tableDef) {
        Database database = tableDef.getDatabase();
        try {
            String dropTableStatement = "truncate table " + tableDef.getFullyQualifiedName();
            database.getCurrentConnection().createStatement().execute(dropTableStatement);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteAllRecordsTable(TableDef tableDef) {
        Database database = tableDef.getDatabase();
        try {
            String dropTableStatement = "delete from " + tableDef.getFullyQualifiedName();
            database.getCurrentConnection().createStatement().execute(dropTableStatement);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {
        Database database = foreignKeyDef.getTableDef().getDatabase();
        try {
            String dropTableStatement = "ALTER TABLE " + foreignKeyDef.getTableDef().getFullyQualifiedName() + " DROP CONSTRAINT " + foreignKeyDef.getName();
            database.getCurrentConnection().createStatement().execute(dropTableStatement);
            LOGGER.info("Foreign Key: " + foreignKeyDef.getName() + " on the table " + foreignKeyDef.getTableDef().getFullyQualifiedName() + " were deleted.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
