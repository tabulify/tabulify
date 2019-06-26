package net.bytle.db.sqlite;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.database.SqlDatabase;
import net.bytle.db.engine.DbDdl;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.model.TableDef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class SqliteSqlDatabase extends SqlDatabase {

    private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<Integer,DataTypeDatabase>();

    static {
        dataTypeDatabaseSet.put(SqliteTypeText.TYPE_CODE, new SqliteTypeText());
    }

    @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return dataTypeDatabaseSet.get(typeCode);
    }

    private final SqliteProvider sqliteProvider;

    /**
     * Returns the provider that created this work system.
     *
     * @return The provider that created this work
     */
    public SqliteSqlDatabase(SqliteProvider sqliteProvider) {
        super(sqliteProvider);
        this.sqliteProvider = sqliteProvider;
    }


    @Override
    public String getNormativeSchemaObjectName(String objectName) {
        return "\"" + objectName + "\"";
    }

    @Override
    public Integer getMaxWriterConnection() {
        return 1;
    }

    /**
     * Returns statement to create the table
     * SQLlite has limited support on the alter statement
     * The primary key shoud be in the create statement.
     * See https://sqlite.org/faq.html#q11
     *
     * @param tableDef
     * @return a create statement https://www.sqlite.org/lang_createtable.html
     */
    @Override
    public List<String> getCreateTableStatements(TableDef tableDef, String name) {

        List<String> statements = new ArrayList<>();
        StringBuilder statement = new StringBuilder();
        statement.append("CREATE TABLE " + getNormativeSchemaObjectName(name) + " (\n");

        for (int i = 0; i < tableDef.getColumnDefs().size(); i++) {
            ColumnDef columnDef = tableDef.getColumnDefs().get(i);
            statement.append(DbDdl.getColumnStatementForCreateTable(columnDef, columnDef.getRelationDef().getSchema()));
            if (i != tableDef.getColumnDefs().size() - 1) {
                statement.append(",\n");
            }
        }

        // Pk
        final PrimaryKeyDef primaryKey = tableDef.getPrimaryKey();
        if (tableDef.getPrimaryKey().getColumns().size() != 0) {
            statement.append(",\nPRIMARY KEY (");
            for (int i = 0; i < primaryKey.getColumns().size(); i++) {
                ColumnDef columnDef = primaryKey.getColumns().get(i);
                statement.append(columnDef.getColumnName());
                if (i < primaryKey.getColumns().size() - 1) {
                    statement.append(", ");
                }
            }
            statement.append(")");
        }

        // Fk
        // http://www.hwaci.com/sw/sqlite/foreignkeys.html
        // The parent table is the table that a foreign key constraint refers to.
        // The child table is the table that a foreign key constraint is applied to and the table that contains the REFERENCES clause.
        //        CREATE TABLE song(
        //                songid     INTEGER,
        //                songartist TEXT,
        //                songalbum TEXT,
        //                songname   TEXT,
        //                FOREIGN KEY(songartist, songalbum) REFERENCES album(albumartist, albumname)
        //        );
        final List<ForeignKeyDef> foreignKeyDefs = tableDef.getForeignKeys();
        for (ForeignKeyDef foreignKeyDef : foreignKeyDefs) {

            statement.append(",\nFOREIGN KEY (");

            // Child columns
            List<String> childColumns = foreignKeyDef.getChildColumns().stream()
                    .map(s -> s.getColumnName())
                    .collect(Collectors.toList());
            statement.append(String.join(",", childColumns));

            statement.append(") REFERENCES ");
            statement.append(foreignKeyDef.getForeignPrimaryKey().getTableDef().getName());
            statement.append("(");

            // Foreign / Parent  columns
            List<String> parentColumns = foreignKeyDef.getForeignPrimaryKey().getColumns()
                    .stream()
                    .map(s -> s.getColumnName())
                    .collect(Collectors.toList());
            statement.append(String.join(",", parentColumns));
            statement.append(")");

        }


        // End statement
        statement.append("\n)");
        statements.add(statement.toString());

        return statements;

    }

    /**
     * Due to a bug in JDBC
     * (ie the primary column names had the foreign key - it seems that they parse the create statement)
     * We created this hack
     *
     * @param tableDef
     */
    public Boolean addPrimaryKey(TableDef tableDef) {

        Connection connection = tableDef.getDatabase().getCurrentConnection();
        List<String> columns = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableDef.getName() + ")");
            while (resultSet.next()) {
                int pk = resultSet.getInt("pk");
                if (pk == 1) {
                    columns.add(resultSet.getString("name"));
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (columns.size() > 0) {
            tableDef.addPrimaryKey(columns);
        }

        return true;

    }

    public Boolean addForeignKey(TableDef tableDef) {

        Connection connection = tableDef.getDatabase().getCurrentConnection();
        Map<Integer, List<String>> foreignKeys = new HashMap<>();
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(" + tableDef.getName() + ")");
        ) {
            while (resultSet.next()) {
                String parentTable = resultSet.getString("table");
                String fromColumn = resultSet.getString("from");
                Integer id = resultSet.getInt("id");
                foreignKeys.put(id, Arrays.asList(parentTable, fromColumn));
            }
        } catch (SQLException e) {
            if (!e.getMessage().equals("query does not return ResultSet")) {
                throw new RuntimeException(e);
            }

        }

        // Sqlite seems to preserve the order of the foreign keys but descendant
        // Hack to get it right
        for (int i = foreignKeys.size() - 1; i >= 0; i--) {
            tableDef.addForeignKey(tableDef.getSchema().getTableOf(foreignKeys.get(i).get(0)), foreignKeys.get(i).get(1));
        }


        return true;

    }

    /**
     * Add columns to a table
     * This function was created because Sqlite does not really implements a JDBC type
     * Sqlite gives them back via a string
     *
     * @param tableDef
     * @return true if the columns were added to the table
     */
    @Override
    public boolean addColumns(TableDef tableDef) {

        // Because the driver returns 20000000 and no data type name
        try (Statement statement = tableDef.getDatabase().getCurrentConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info('" + tableDef.getName() + "')");
        ) {
            while (resultSet.next()) {
                // ie INTEGER(50)
                String dataType = resultSet.getString("type");
                SqliteType type = SqliteType.get(dataType);
                final String typeCodeName = type.getTypeName();
                Integer typeCode = type.getTypeCode();
                // SQlite use class old data type
                if (typeCodeName.equals("TEXT")) {
                    typeCode = DataTypesJdbc.of("VARCHAR").getTypeCode();
                }
                tableDef.getColumnOf(resultSet.getString("name"))
                        .typeCode(typeCode)
                        .precision(type.getPrecision())
                        .scale(type.getScale())
                        .isAutoincrement("")
                        .isGeneratedColumn("")
                        .setNullable(resultSet.getInt("notnull") == 0 ? 1 : 0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     *
     * @param path whatever/youwant/db.db
     * @return an JDBC Url from a path
     */
    static public String getJdbcUrl(Path path){

        Path dirDbFile = path.getParent();
        if (!Files.exists(dirDbFile)) {
            try {
                Files.createDirectory(dirDbFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        // TODO: what if linux
        String rootWindows = "///";
        return "jdbc:sqlite:" + rootWindows + path.toString().replace("\\", "/");

    }

}
