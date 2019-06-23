package net.bytle.db.stream;

import net.bytle.db.model.TableDef;

import java.util.List;

public class MemoryInsertStream extends InsertStreamAbs implements InsertStream {

    private final TableDef tableDef;

    private List<List<Object>> tableValues;

    private MemoryInsertStream(TableDef tableDef) {
        this.tableDef = tableDef;
        tableValues = StorageManager.get(tableDef);
    }

    public static InsertStream get(TableDef tableDef) {
        return new MemoryInsertStream(tableDef);
    }

    @Override
    public MemoryInsertStream insert(List<Object> values) {

        if (tableDef.getColumnDefs().size() < values.size()) {
            while (tableDef.getColumnDefs().size() < values.size()) {
                tableDef.addColumn(String.valueOf(tableDef.getColumnDefs().size()));
            }
        }
        tableValues.add(values);
        return this;

    }


    @Override
    public void close() {
        // Nothing to do here because the data are in memory
        // and don't want them to disappear
        // There is no connection
    }

    /**
     * In case of parent child hierarchy
     * we can check if we need to send the data with the function nextInsertSendBatch()
     * and send it with this function
     */
    @Override
    public void flush() {

    }




}
