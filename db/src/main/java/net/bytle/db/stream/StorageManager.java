package net.bytle.db.stream;

import net.bytle.db.DbLoggers;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class StorageManager {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    static private Map<TableDef, List<List<Object>>> tableValues = new HashMap<>();

    public static List<List<Object>> get(TableDef tableDef) {
        return tableValues.computeIfAbsent(tableDef, k -> new ArrayList<>());
    }

    public static void delete(TableDef tableDef) {
        List<List<Object>> values = tableValues.remove(tableDef);
        if (values == null) {
            LOGGER.warning("The table (" + tableDef + ") had no values. Nothing removed.");
        }
    }

    public static void drop(TableDef tableDef) {
        StorageManager.delete(tableDef);
        Tables.dropCache(tableDef);
    }
}
