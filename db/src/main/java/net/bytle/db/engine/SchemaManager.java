package net.bytle.db.engine;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.cli.Log;

import java.util.List;

public class SchemaManager {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    public static void printTables(SchemaDef schemaDef) {

        List<TableDef> tables = schemaDef.getTables();
        if (tables.size()==0){
            LOGGER.info("The schema ("+schemaDef.getName()+") has no tables.");
        } else {
            for (TableDef tableDef : tables) {
                Integer rowCount = Tables.getSize(tableDef);
                System.out.println(tableDef.getName() + "\t" + rowCount);
            }
        }

    }


    /**
     * @return the null schema
     */
    public static SchemaDef getNull(Database database) {
        return database.getSchema("null");
    }

    public static void dropAllTables(SchemaDef schemaDef) {
        List<TableDef> tables = schemaDef.getTables();
        Tables.dropIfExist(tables);
    }
}
