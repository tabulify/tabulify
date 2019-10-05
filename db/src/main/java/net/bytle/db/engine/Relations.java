package net.bytle.db.engine;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;

import java.sql.ResultSet;

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

}
