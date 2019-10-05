package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.cli.Log;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.Streams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Memories {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    static private Map<MemoryTable, List<List<Object>> > tableValues = new HashMap<>();

    public static List<List<Object>>  get(MemoryTable memoryTable) {
        return tableValues.computeIfAbsent(memoryTable, k -> new ArrayList<>());
    }

    public static void delete(MemoryTable memoryTable) {
        Object values = tableValues.remove(memoryTable);
        if (values == null) {
            LOGGER.warning("The table (" + memoryTable + ") had no values. Nothing removed.");
        }
    }

    public static void drop(MemoryTable memoryTable) {
        Memories.delete(memoryTable);
    }

    public static void truncate(MemoryTable memoryTable) {
        tableValues.put(memoryTable,new ArrayList<>());
    }

    /**
     * Print the data of a table
     *
     * @param memoryTable
     */
    public static void print(MemoryTable memoryTable) {
        ListSelectStream tableOutputStream = ListSelectStream.of(memoryTable);
        Streams.print(tableOutputStream);
        tableOutputStream.close();
    }

    public static InsertStream getInsertStream(MemoryTable memoryTable) {
        return ListInsertStream.of(memoryTable);
    }

    public static SelectStream getSelectStream(MemoryTable memoryTable) {
        return ListSelectStream.of(memoryTable);
    }
}
