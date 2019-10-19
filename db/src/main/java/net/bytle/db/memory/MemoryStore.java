package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;

import java.util.*;

public class MemoryStore<T extends Collection<? extends List>> {

    private Map<DataPath, T> tableValues = new HashMap<>();

    public T getValues(DataPath memoryTable) {

        return tableValues.get(memoryTable);
    }

    public T remove(DataPath memoryTable) {
        return tableValues.remove(memoryTable);
    }

    public void put(DataPath memoryTable, T objects) {
        tableValues.put(memoryTable,objects);
    }

    public Boolean containsKey(DataPath dataPath) {
        return tableValues.containsValue(dataPath);
    }
}
