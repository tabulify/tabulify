package net.bytle.db.stream;

import net.bytle.db.model.TableDef;

import java.sql.Clob;
import java.util.List;

/**
 * Select stream of a table saved as List<List<Object>>
 */
public class MemorySelectStream implements SelectStream {

    private final List<List<Object>> values;
    private final TableDef tableDef;
    private int index = -1;

    private MemorySelectStream(TableDef tableDef) {
        this.tableDef = tableDef;
        this.values = StorageManager.get(tableDef);
    }

    public static MemorySelectStream get(TableDef tableDef) {
        return new MemorySelectStream(tableDef);
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
            return o.toString();
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
    public TableDef getRelationDef() {
        return tableDef;
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
