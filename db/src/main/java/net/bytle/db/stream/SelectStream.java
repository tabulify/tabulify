package net.bytle.db.stream;

import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;

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


    DataPath getDataPath();

    double getDouble(int columnIndex);

    Clob getClob(int columnIndex);
}
