package net.bytle.db.spi;

import net.bytle.db.memory.ListInsertStream;
import net.bytle.db.memory.MemoryStore;
import net.bytle.db.memory.MemoryTable;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;

public class DataDefs {

    /**
     * Add the columns to the targetDef from the sourceDef
     *
     * @param targetDef
     * @param sourceDef
     */
    public static void addColumns(RelationDef targetDef, RelationDef sourceDef) {

        // Add the columns
        int columnCount = sourceDef.getColumnDefs().size();
        for (int i = 0; i < columnCount; i++) {
            ColumnDef columnDef = sourceDef.getColumnDef(i);
            targetDef.getColumnOf(columnDef.getColumnName(),columnDef.getClass())
                    .typeCode(columnDef.getDataType().getTypeCode())
                    .precision(columnDef.getPrecision())
                    .scale(columnDef.getScale());
        }

    }

    public static void printColumns(TableDef tableDef) {

        MemoryTable tableStructure = MemoryTable.of("structure")
                .addColumn("#")
                .addColumn("Colum Name")
                .addColumn("Data Type")
                .addColumn("Key")
                .addColumn("Not Null")
                .addColumn("Default")
                .addColumn("Auto Increment")
                .addColumn("Description");

        InsertStream insertStream = ListInsertStream.of(tableStructure);
        int i = 0;
        for (ColumnDef columnDef : tableDef.getColumnDefs()) {
            i++;
            insertStream.insert(
                    i,
                    columnDef.getColumnName(),
                    columnDef.getDataType().getTypeName(),
                    (tableDef.getPrimaryKey().getColumns().contains(columnDef) ? "x" : ""),
                    (columnDef.getNullable() == 0 ? "x" : ""),
                    columnDef.getDefault(),
                    columnDef.getIsAutoincrement(),
                    columnDef.getDescription()

            );
        }
        insertStream.close();


        MemoryStore.of().print(tableStructure);
        MemoryStore.of().drop(tableStructure);
    }
}
