package net.bytle.db.memory;

import net.bytle.db.stream.SelectStream;

import java.sql.Clob;
import java.util.List;

/**
 * Select stream of a table saved as List<List<Object>>
 */
public class ListSelectStream implements SelectStream {

    private final List<List<Object>> values;
    private final MemoryTable memoryTable;
    private int index = -1;

    private ListSelectStream(MemoryTable memoryTable) {
        this.memoryTable = memoryTable;
        this.values = Memories.get(memoryTable);
    }

    public static ListSelectStream of(MemoryTable memoryTable) {
        return new ListSelectStream(memoryTable);
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
        final List<Object> row = values.get(index);
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
    public MemoryTable getRelationDef() {
        return memoryTable;
    }

    @Override
    public double getDouble(int columnIndex) {
        return (double) getObject(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) {
        throw new RuntimeException("Not Yet Implemented");
    }

}
