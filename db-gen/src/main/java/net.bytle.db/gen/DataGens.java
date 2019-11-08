package net.bytle.db.gen;

import net.bytle.log.Log;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.util.ArrayList;
import java.util.List;

/**
 * Static Utility
 */
public class DataGens {

    private static final Log LOGGER = DataGeneration.GEN_LOG;

    public static void suppressSelfReferencingForeignKeys(DataPath schemaDef) {

        int counter = 0;
        for (ForeignKeyDef foreignKeyDef : getSelfReferencingForeignKeys(schemaDef)) {
            counter++;
            Tabulars.dropForeignKey(foreignKeyDef);
        }
        if (counter==0){
            LOGGER.info("No self referencing foreign keys was found.");
        } else {
            LOGGER.info(counter+" self referencing foreign keys was found and suppressed.");
        }

    }

    public static List<ForeignKeyDef> getSelfReferencingForeignKeys(DataPath dataPath) {


        List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
        List<DataPath> dataPathToChecks = new ArrayList<>();
        if (Tabulars.isContainer(dataPath)) {
            dataPathToChecks.addAll(Tabulars.getChildrenDataPath(dataPath));
        } else {
            dataPathToChecks.add(dataPath);
        }

        for (DataPath dataPathToCheck : dataPathToChecks) {
            for (ForeignKeyDef foreignKeyDef : dataPathToCheck.getDataDef().getForeignKeys()) {
                if (dataPathToCheck.equals(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath())) {
                    foreignKeyDefs.add(foreignKeyDef);
                }
            }
        }
        return foreignKeyDefs;

    }


    public static List<ForeignKeyDef> getSecondForeignKeysOnTheSameColumn(DataPath schemaPath) {

        List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
        for (DataPath dataPath : Tabulars.getChildrenDataPath(schemaPath)) {
            List<ColumnDef> columnDefs = new ArrayList<>();
            for (ForeignKeyDef foreignKeyDef : dataPath.getDataDef().getForeignKeys()) {
                for (ColumnDef columnDef : foreignKeyDef.getChildColumns()) {
                    if (columnDefs.contains(columnDef)){
                        foreignKeyDefs.add(foreignKeyDef);
                    } else {
                        columnDefs.add(columnDef);
                    }
                }
            }
        }
        return foreignKeyDefs;

    }


    public static void suppressSecondForeignKeysOnTheSameColumn(DataPath schemaDef) {

        int counter = 0;
        for (ForeignKeyDef foreignKeyDef : getSecondForeignKeysOnTheSameColumn(schemaDef)) {
            counter++;
            Tabulars.dropForeignKey(foreignKeyDef);
        }
        if (counter==0){
            LOGGER.info("No more than one foreign key on the same column was found.");
        } else {
            LOGGER.info(counter+" second foreign keys on the same column was deleted.");
        }

    }
}
