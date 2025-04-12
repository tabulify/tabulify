package com.tabulify.gen;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import net.bytle.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Static Utility
 */
public class DataGens {

  private static final Log LOGGER = GenLog.LOGGER;

  public static void suppressSelfReferencingForeignKeys(DataPath schemaDef) {

    int counter = 0;
    for (ForeignKeyDef foreignKeyDef : getSelfReferencingForeignKeys(schemaDef)) {
      counter++;
      DataPath foreignKeyDataPath = foreignKeyDef.getRelationDef().getDataPath();
      Tabulars.dropOneToManyRelationship(foreignKeyDataPath, foreignKeyDataPath);
    }
    if (counter == 0) {
      LOGGER.info("No self referencing foreign keys was found.");
    } else {
      LOGGER.info(counter + " self referencing foreign keys was found and suppressed.");
    }

  }

  /**
   * Not supported a foreign key that references it self
   *
   * @param dataPath
   * @return
   */
  public static List<ForeignKeyDef> getSelfReferencingForeignKeys(DataPath dataPath) {


    List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
    List<DataPath> dataPathToChecks = new ArrayList<>();
    if (Tabulars.isContainer(dataPath)) {
      dataPathToChecks.addAll(Tabulars.getChildren(dataPath));
    } else {
      dataPathToChecks.add(dataPath);
    }

    for (DataPath dataPathToCheck : dataPathToChecks) {
      for (ForeignKeyDef foreignKeyDef : dataPathToCheck.getOrCreateRelationDef().getForeignKeys()) {
        if (dataPathToCheck.equals(foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath())) {
          foreignKeyDefs.add(foreignKeyDef);
        }
      }
    }
    return foreignKeyDefs;

  }


  /**
   * Two foreign key definition on the same column
   * @param dataPath
   * @return
   */
  public static List<ForeignKeyDef> getSecondForeignKeysOnTheSameColumn(DataPath dataPath) {

    List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
    List<DataPath> dataPathToChecks = new ArrayList<>();
    if (Tabulars.isContainer(dataPath)) {
      dataPathToChecks.addAll(Tabulars.getChildren(dataPath));
    } else {
      dataPathToChecks.add(dataPath);
    }

    for (DataPath dataPathToCheck : dataPathToChecks) {
      List<ColumnDef> columnDefs = new ArrayList<>();
      for (ForeignKeyDef foreignKeyDef : dataPathToCheck.getOrCreateRelationDef().getForeignKeys()) {
        for (ColumnDef columnDef : foreignKeyDef.getChildColumns()) {
          if (columnDefs.contains(columnDef)) {
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
      counter = counter + 1;
      Tabulars.dropOneToManyRelationship(foreignKeyDef);
    }
    //noinspection ConstantConditions
    if (counter == 0) {
      LOGGER.info("No more than one foreign key on the same column was found.");
    } else {
      LOGGER.info(counter + " second foreign keys on the same column was deleted.");
    }

  }

  public static GenColumnDef castToGenColumnDef(ColumnDef c) {
    return (GenColumnDef) c;
  }

}
