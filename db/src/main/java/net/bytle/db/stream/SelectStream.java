package net.bytle.db.stream;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    /**
     * Retrieves and removes the head of this data path, or returns null if this queue is empty.
     * @param i
     * @param timeUnit
     * @return
     */
    List<Object> poll(int i, TimeUnit timeUnit);

    Integer getInteger(int columnIndex);

}
