package net.bytle.db.stream;

import net.bytle.db.model.RelationDef;

import java.sql.Clob;

public interface SelectStream extends AutoCloseable {

    boolean next();

    void close();

    String getString(int columnIndex);

    void beforeFirst();

    boolean first();

    boolean last();

    int getRow();

    boolean previous();

    Object getObject(int columnIndex);

    RelationDef getRelationDef();

    double getDouble(int columnIndex);

    Clob getClob(int columnIndex);
}
