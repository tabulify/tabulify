package net.bytle.db.engine;

import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class Relations {


    public static void addColumns(RelationDef relationDef, ResultSet resultSet) {

        ResultSets.addColumns(resultSet, relationDef);

    }

    public static void addColumns(RelationDef sourceDef, RelationDef targetDef) {

        // Add the columns
        for (int i = 0; i < sourceDef.getColumnDefs().size(); i++) {
            ColumnDef columnDef = sourceDef.getColumnDef(i);
            targetDef.getColumnOf(columnDef.getColumnName(), columnDef.getClazz())
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


    public static void copy(DataPath source, DataPath target) {
        addColumns(source.getDataDef(), target.getDataDef());
        final PrimaryKeyDef sourcePrimaryKey = source.getDataDef().getPrimaryKey();
        if (sourcePrimaryKey!=null) {
            final List<String> columns = sourcePrimaryKey.getColumns().stream()
                    .map(s->s.getColumnName())
                    .collect(Collectors.toList());
            target.getDataDef().setPrimaryKey(columns);
        }
    }
}
