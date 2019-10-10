package net.bytle.db.spi;

import net.bytle.db.model.RelationDef;
import net.bytle.db.uri.DataUri;

public abstract class TableSystem {

    public abstract RelationDef getRelationDef(DataUri dataUri);

}
