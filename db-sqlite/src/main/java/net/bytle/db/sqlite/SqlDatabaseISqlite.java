package net.bytle.db.sqlite;

import net.bytle.db.database.Database;
import net.bytle.db.database.SqlDatabase;
import net.bytle.db.engine.DbDdl;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.model.TableDef;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by gerard on 28-11-2015.
 */
public class SqlDatabaseISqlite extends SqlDatabase {


    public SqlDatabaseISqlite(Database database) {
        super(database);
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
     *
     * @param tableDef
     * @return http://www.hwaci.com/sw/sqlite/lang_createtable.html
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
                statement.append(", \n");
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
        statement.append("\n)\n");
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
        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(" + tableDef.getName() + ")");
        ) {
            while (resultSet.next()) {
                // https://sqlite.org/pragma.html#pragma_foreign_key_list
                // One row by constraint
                String parentTable = resultSet.getString("table");
                String fromColumn = resultSet.getString("from");
                tableDef.addForeignKey(parentTable, fromColumn);
            }
        } catch (SQLException e) {
            if (!e.getMessage().equals("query does not return ResultSet")) {
                throw new RuntimeException(e);
            }

        }

        return true;

    }

}
