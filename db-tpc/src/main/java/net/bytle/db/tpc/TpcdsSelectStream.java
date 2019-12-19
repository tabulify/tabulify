package net.bytle.db.tpc;

import com.teradata.tpcds.Options;
import com.teradata.tpcds.Results;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamListener;

import java.sql.Clob;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.bytle.db.tpc.TpcdsDgenTable.LOGGER;

public class TpcdsSelectStream implements SelectStream {

    private final DataPath dataPath;
        // Tpcdsd data
    private Iterator<List<List<String>>> results;
    private List<String> values;
    private int row = 0;
    private SelectStreamListener selectStreamListener;
    private String name;


    public TpcdsSelectStream(DataPath dataPath) {
        this.dataPath = dataPath;

        init();
    }

    private void init() {
        // Teradata
        Options options = new Options();
        Session session = options.toSession();
        // Could be parallized with
        // session.withChunkNumber(chunkNumber)
        options.table = dataPath.getName();

        Table table;
        if (!dataPath.getName().startsWith("s_")) {
            table = Table.getTable(dataPath.getName());
        } else {
            String msg = "The staging table are not yet supported. Data Path ("+dataPath+") cannot be read.";
            LOGGER.severe(msg);
            throw new RuntimeException(msg);
            // Ter info
            //                    table = Table.getSourceTables()
            //                            .stream()
            //                            .filter(s -> s.getName().toLowerCase().equals(tableDef.getName().toLowerCase()))
            //                            .collect(Collectors.toList())
            //                            .of(0);
        }

        // If this is a child table and not the only table being generated, it will be generated when its parent is generated, so move on.
        if (table.isChild() && !session.generateOnlyOneTable()) {
            throw new RuntimeException("This table is a child table and should be loaded with its parent. Not yet supported");
        }
        if (table.hasChild()){
            throw new RuntimeException("This table ("+dataPath+") is a parent table and should be loaded with its children ("+ Tabulars.getReferences(dataPath)+"). Not yet supported");
        }
        results = Results.constructResults(table, session).iterator();
    }

    public static TpcdsSelectStream of(DataPath dataPath) {
        return new TpcdsSelectStream(dataPath);
    }

    @Override
    public boolean next() {
        if (results.hasNext()){
            row++;
            values = results.next().get(0);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getString(int columnIndex) {
        return values.get(columnIndex);
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public Object getObject(int columnIndex) {
        return values.get(columnIndex);
    }

    @Override
    public TableDef getSelectDataDef() {
        return dataPath.getDataDef();
    }

    @Override
    public double getDouble(int columnIndex) {
        return Double.parseDouble(values.get(columnIndex));
    }

    @Override
    public Clob getClob(int columnIndex) {
        throw new RuntimeException("Tpcds does not have a clob data type");
    }

    @Override
    public boolean next(Integer timeout, TimeUnit timeUnit) {
        return next();
    }

    @Override
    public Integer getInteger(int columnIndex) {
        return Integer.valueOf(values.get(columnIndex));
    }

    @Override
    public Object getObject(String columnName) {
        int i = DataDefs.getColumnIdFromName(dataPath.getDataDef(), columnName);
        return values.get(i);
    }

    @Override
    public SelectStreamListener getSelectStreamListener() {
        if (selectStreamListener == null){
            selectStreamListener = SelectStreamListener.of(this);
        }
        return selectStreamListener;
    }

    @Override
    public List<Object> getObjects() {
        return Collections.singletonList(values);
    }

    @Override
    public SelectStream setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public void beforeFirst() {
        init();
    }
}
