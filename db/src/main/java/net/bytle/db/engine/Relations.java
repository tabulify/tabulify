package net.bytle.db.engine;

import net.bytle.db.model.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

public class Relations {



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


    public static void copy(TableDef source, TableDef target) {
        addColumns(source, target);
        final PrimaryKeyDef sourcePrimaryKey = source.getPrimaryKey();
        if (sourcePrimaryKey!=null) {
            final List<String> columns = sourcePrimaryKey.getColumns().stream()
                    .map(s->s.getColumnName())
                    .collect(Collectors.toList());
            target.setPrimaryKey(columns);
        }
    }
}
