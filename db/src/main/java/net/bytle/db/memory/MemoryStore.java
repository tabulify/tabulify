package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.cli.Log;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.Streams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MemoryStore  {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    static private Map<DataPath, List<List<Object>> > tableValues = new HashMap<>();
    private static MemoryStore staticMemoryStore;

    public static List<List<Object>>  get(DataPath memoryTable) {
        return tableValues.computeIfAbsent(memoryTable, k -> new ArrayList<>());
    }

    public static MemoryStore of() {
        if (staticMemoryStore == null){
            staticMemoryStore = new MemoryStore();
        }
        return staticMemoryStore;
    }

    public void delete(DataPath memoryTable) {
        Object values = tableValues.remove(memoryTable);
        if (values == null) {
            LOGGER.warning("The table (" + memoryTable + ") had no values. Nothing removed.");
        }
    }

    public void drop(DataPath memoryTable) {
        delete(memoryTable);
    }

    public  void truncate(DataPath memoryTable) {
        tableValues.put(memoryTable,new ArrayList<>());
    }

    /**
     * Print the data of a table
     *
     * @param memoryTable
     */
    public void print(DataPath memoryTable) {
        ListSelectStream tableOutputStream = ListSelectStream.of(memoryTable);
        Streams.print(tableOutputStream);
        tableOutputStream.close();
    }

    public InsertStream getInsertStream(DataPath memoryTable) {
        return ListInsertStream.of(memoryTable);
    }

    public SelectStream getSelectStream(DataPath memoryTable) {
        return ListSelectStream.of(memoryTable);
    }
}
