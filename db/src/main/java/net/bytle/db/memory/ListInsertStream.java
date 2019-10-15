package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;

import java.util.List;

public class ListInsertStream extends InsertStreamAbs implements InsertStream {

    private final MemoryDataPath memoryTable;

    private List<List<Object>> tableValues;

    private ListInsertStream(DataPath memoryTable) {
        super(memoryTable);
        this.memoryTable = (MemoryDataPath) memoryTable;
        tableValues = MemoryStore.get(memoryTable);
    }

    public static InsertStream of(DataPath memoryTable) {
        return new ListInsertStream(memoryTable);
    }

    @Override
    public ListInsertStream insert(List<Object> values) {

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
