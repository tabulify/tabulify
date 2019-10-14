package net.bytle.db.stream;

import net.bytle.db.model.TableDef;

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


    TableDef getDataDef();

    double getDouble(int columnIndex);

    Clob getClob(int columnIndex);

}
