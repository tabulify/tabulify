package net.bytle.db.memory;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListInsertStreamOld extends InsertStreamAbs implements InsertStream {

    private final MemoryDataPath memoryTable;

    private List<List<Object>> tableValues;

    ListInsertStreamOld(MemoryDataPath memoryDataPath) {
        super(memoryDataPath);
        this.memoryTable = memoryDataPath;
        final MemoryStore memoryStore = memoryDataPath.getDataSystem().getMemoryStore();
        Collection collection = memoryStore.getValues(memoryDataPath);
        if (collection==null){
            tableValues = new ArrayList<>();
            memoryStore.put(memoryTable,tableValues);
        } else {
            this.tableValues = (List<List<Object>>) collection;
        }
    }


    @Override
    public ListInsertStreamOld insert(List<Object> values) {

        if (memoryTable.getDataDef().getColumnDefs().size() < values.size()) {
            while (memoryTable.getDataDef().getColumnDefs().size() < values.size()) {
                memoryTable.getDataDef().addColumn(String.valueOf(memoryTable.getDataDef().getColumnDefs().size()));
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
