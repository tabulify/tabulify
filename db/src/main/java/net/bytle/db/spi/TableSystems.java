package net.bytle.db.spi;

import net.bytle.db.memory.MemorySystemProvider;

public class TableSystems {

    public static TableSystem getDefault() {
        return MemorySystemProvider.of().getTableSystem(null);
    }
}
