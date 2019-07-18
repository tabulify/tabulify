package net.bytle.db.stream;

import net.bytle.db.DbLoggers;
import net.bytle.db.model.TableDef;
import net.bytle.cli.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageManager {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

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
    }

    public static void truncate(TableDef tableDef) {
        tableValues.put(tableDef,new ArrayList<>());
    }
}
