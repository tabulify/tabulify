package net.bytle.db.memory;

import net.bytle.db.database.Databases;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.RelationDefAbs;


public class MemoryTable extends RelationDefAbs implements RelationDef {


    public MemoryTable(String name) {
        super(name);
        super.schema = Databases.of("memory").getCurrentSchema();
    }

    public static MemoryTable of(String name) {
        return new MemoryTable(name);
    }

    public MemoryTable addColumn(String name) {
        super.meta.addColumn(name);
        return this;
    }

    public MemoryTable addColumn(String name, int typeCode) {
        super.meta.addColumn(name, typeCode);
        return this;
    }
}
