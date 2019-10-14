package net.bytle.db.engine;

import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.RelationDef;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Relations {


    public static void addColumns(RelationDef relationDef, ResultSet resultSet) {

        ResultSets.addColumns(resultSet, relationDef);

    }

    public static void addColumns(RelationDef targetDef, RelationDef sourceDef) {

        // Add the columns
        for (int i = 0; i < sourceDef.getColumnDefs().size(); i++) {
            ColumnDef columnDef = sourceDef.getColumnDef(i);
            targetDef.getColumnOf(columnDef.getColumnName(), columnDef.getClass())
                    .typeCode(columnDef.getDataType().getTypeCode())
                    .precision(columnDef.getPrecision())
                    .scale(columnDef.getScale())
                    .comment(columnDef.getComment());
        }


    }

    /**
     * Dropping a foreign key
     *
     * @param foreignKeyDef
     */
    public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {

        foreignKeyDef.getTableDef().getDataPath().getDataSystem().dropForeignKey(foreignKeyDef);


    }



}
