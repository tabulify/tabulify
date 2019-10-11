package net.bytle.db.spi;

import net.bytle.db.model.RelationDef;

public abstract class DataPath {


    public abstract TableSystem getDataSystem();

    public abstract RelationDef getDataDef();

    public abstract String getName();

}
