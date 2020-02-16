package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;

import java.util.*;

public class MemoryStore {

    private Map<MemoryDataPath, Object> tableValues = new HashMap<>();

    public Object getValue(DataPath memoryTable) {

        return tableValues.get(memoryTable);
    }

    public Object remove(DataPath memoryTable) {
        return tableValues.remove(memoryTable);
    }

    public void put(MemoryDataPath memoryDataPath, Object objects) {
        tableValues.put(memoryDataPath,objects);
    }

    public Boolean containsKey(DataPath dataPath) {
        return tableValues.containsKey(dataPath);
    }

}
