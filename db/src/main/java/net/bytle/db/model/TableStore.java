package net.bytle.db.model;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

public interface TableStore {

    void delete(RelationDef relationDef);

    void truncate(RelationDef relationDef);

    InsertStream getInsertStream(RelationDef relationDef);

    SelectStream getSelectStream(RelationDef relationDef);

}
