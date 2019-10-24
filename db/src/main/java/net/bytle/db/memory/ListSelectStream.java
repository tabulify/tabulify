package net.bytle.db.memory;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * List implementation
 */
public class ListSelectStream extends SelectStreamAbs implements SelectStream {

    private List<List<Object>> values;
    private final DataPath memoryTable;
    private int index = -1;

    ListSelectStream(MemoryDataPath memoryTable) {
        this.memoryTable = memoryTable;
        MemoryStore memoryStore = memoryTable.getDataSystem().getMemoryStore();
        Collection collection = memoryStore.getValues(memoryTable);
        if (collection==null) {
            this.values = new ArrayList<>();
            memoryStore.put(memoryTable,this.values);
        } else {
            this.values = (List<List<Object>>) collection;
        }
    }




    @Override
    public boolean next() {

        if (index >= values.size() - 1) {
            return false;
        } else {
            index++;
            return true;
        }

    }


    @Override
    public void close() {

    }



    @Override
    public String getString(int columnIndex) {
        List<Object> row = values.get(index);
        final int index = columnIndex;
        if (index < row.size()) {
            final Object o = row.get(index);
            if (o==null){
                return null;
            } else {
                return o.toString();
            }
        } else {
            return "";
        }
    }



    @Override
    public void beforeFirst() {
        index = -1;
    }



    @Override
    public boolean first() {
        index = 0;
        return true;
    }


    @Override
    public boolean last() {
        index = values.size() - 1;
        return true;
    }


    @Override
    public int getRow() {
        return index + 1;
    }



    @Override
    public boolean previous() {
        if (index <= 0) {
            return false;
        } else {
            index--;
            return true;
        }
    }



    @Override
    public Object getObject(int columnIndex) {
        return values.get(index).get(columnIndex);
    }

    @Override
    public TableDef getDataDef() {
        return memoryTable.getDataDef();
    }


    @Override
    public double getDouble(int columnIndex) {
        return (double) getObject(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) {
        throw new RuntimeException("Not Yet Implemented");
    }

    /**
     * Retrieves and removes the head of this data path, or returns null if this queue is empty.
     *
     * @param i
     * @param timeUnit
     * @return
     */
    @Override
    public List<Object> poll(int i, TimeUnit timeUnit) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public Integer getInteger(int columnIndex) {
        return (Integer) getObject(columnIndex);
    }

    @Override
    public Object getObject(String columnName) {
        throw new RuntimeException("Not yet implemented");
    }
}
