package net.bytle.db.gen;

import net.bytle.db.engine.DbDdl;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Static Utility
 */
public class DataGens {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    public static void suppressSelfReferencingForeignKeys(SchemaDef schemaDef) {

        int counter = 0;
        for (ForeignKeyDef foreignKeyDef : getSelfReferencingForeignKeys(schemaDef)) {
            counter++;
            DbDdl.dropForeignKey(foreignKeyDef);
        }
        if (counter==0){
            LOGGER.info("No self referencing foreign keys was found.");
        } else {
            LOGGER.info(counter+" self referencing foreign keys was found and suppressed.");
        }

    }

    public static List<ForeignKeyDef> getSelfReferencingForeignKeys(SchemaDef schemaDef) {

        List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
        for (TableDef tableDef : schemaDef.getTables()) {
            foreignKeyDefs.addAll(getSelfReferencingForeignKeys(tableDef));
        }
        return foreignKeyDefs;

    }

    public static List<ForeignKeyDef> getSelfReferencingForeignKeys(TableDef tableDef) {
        List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
        for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
            if (tableDef.equals(foreignKeyDef.getForeignPrimaryKey().getTableDef())) {
                foreignKeyDefs.add(foreignKeyDef);
            }
        }
        return foreignKeyDefs;
    }


    public static List<ForeignKeyDef> getSecondForeignKeysOnTheSameColumn(SchemaDef schemaDef) {

        List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
        for (TableDef tableDef : schemaDef.getTables()) {
            foreignKeyDefs.addAll(getSecondForeignKeysOnTheSameColumn(tableDef));
        }
        return foreignKeyDefs;

    }

    public static List<ForeignKeyDef> getSecondForeignKeysOnTheSameColumn(TableDef tableDef) {
        List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
        List<ColumnDef> columnDefs = new ArrayList<>();
        for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
            for (ColumnDef columnDef : foreignKeyDef.getChildColumns()) {
                if (columnDefs.contains(columnDef)){
                    foreignKeyDefs.add(foreignKeyDef);
                } else {
                    columnDefs.add(columnDef);
                }
            }
        }
        return foreignKeyDefs;
    }

    public static void suppressSecondForeignKeysOnTheSameColumn(SchemaDef schemaDef) {

        int counter = 0;
        for (ForeignKeyDef foreignKeyDef : getSecondForeignKeysOnTheSameColumn(schemaDef)) {
            counter++;
            DbDdl.dropForeignKey(foreignKeyDef);
        }
        if (counter==0){
            LOGGER.info("No more than one foreign key on the same column was found.");
        } else {
            LOGGER.info(counter+" second foreign keys on the same column was deleted.");
        }

    }
}
