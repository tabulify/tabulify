package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;

import java.util.Arrays;
import java.util.List;

public class MemoryDataPath extends DataPath {

    private final String[] names ;
    private final MemoryStore memoryStore;


    public MemoryDataPath(MemoryStore memoryStore, String[] names) {
        this.memoryStore = memoryStore;
        this.names = names;
    }

    public static DataPath of(MemoryStore memoryStore, String[] names) {
        return new MemoryDataPath(memoryStore,names);
    }

    @Override
    public TableSystem getDataSystem() {
        return memoryStore;
    }




    @Override
    public String getName() {
        return names[names.length-1];
    }

    @Override
    public List<String> getPathSegments() {
        return Arrays.asList(names);
    }

}
