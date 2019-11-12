package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;

import java.util.*;

public class MemoryStore {

    private Map<DataPath, Collection<? extends List>> tableValues = new HashMap<>();

    public Collection<? extends List> getValues(DataPath memoryTable) {

        return tableValues.get(memoryTable);
    }

    public Collection<? extends List> remove(DataPath memoryTable) {
        return tableValues.remove(memoryTable);
    }

    public void put(DataPath memoryTable, Collection<? extends List> objects) {
        tableValues.put(memoryTable,objects);
    }

    public Boolean containsKey(DataPath dataPath) {
        return tableValues.containsKey(dataPath);
    }
}
