package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;

import java.util.Arrays;
import java.util.List;

public class MemoryDataPath extends DataPath {

    private final String[] names ;
    private final MemoryDataSystem memoryDataSystem;

    public final static int TYPE_LIST = 0;
    public final static int TYPE_BLOCKED_QUEUE = 1;

    private int type = TYPE_LIST;


    public MemoryDataPath(MemoryDataSystem memoryDataSystem, String[] names) {
        this.memoryDataSystem = memoryDataSystem;
        this.names = names;
    }

    public static DataPath of(MemoryDataSystem memoryDataSystem, String[] names) {
        return new MemoryDataPath(memoryDataSystem,names);
    }

    @Override
    public MemoryDataSystem getDataSystem() {
        return memoryDataSystem;
    }




    @Override
    public String getName() {
        return names[names.length-1];
    }

    @Override
    public List<String> getPathSegments() {
        return Arrays.asList(names);
    }

    public int getType() {
        return type;
    }
}
