package net.bytle.db.spi;

import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;

import java.util.List;
import java.util.Objects;

public abstract class DataSetSystem extends TableSystem {


    public abstract DataPath getDataPath(DataUri dataUri);

    public abstract DataPath getDataPath(String... name);

    public abstract Boolean exists(DataPath dataPath);

    public abstract SelectStream getSelectStream(DataPath dataPath);

    public abstract Database getDatabase();

    public abstract <T> T getMax(ColumnDef<T> columnDef);

    public abstract boolean isContainer(DataPath dataPath);

    public void create(DataPath dataPath){
        throw new RuntimeException("A data set cannot create a data path. It can only read it");
    }

    // The product name (for a jdbc database: sql server, oracle, hive ...
    public abstract String getProductName();

    public abstract DataType getDataType(Integer typeCode);

    public void drop(DataPath dataPath){
        throw new RuntimeException("A data set cannot drop a data path. It can only read it");
    }
    public void delete(DataPath dataPath) {
        throw new RuntimeException("A data set cannot delete a data path. It can only read it");
    }

    public void truncate(DataPath dataPath){
        throw new RuntimeException("A data set cannot truncate a data path. It can only read it");
    }


    public abstract TableSystemProvider getProvider();

    public  InsertStream getInsertStream(DataPath dataPath){
        throw new RuntimeException("A data set cannot insert into a data path. It can only read it");
    }

    public abstract List<DataPath> getChildrenDataPath(DataPath dataPath);

    public void move(DataPath source, DataPath target, TransferProperties transferProperties){
        throw new RuntimeException("A data set cannot move its data paths. It can only read them");
    }

    /**
     *
     * @return The number of thread that can be created against the data system
     */
    public abstract Integer getMaxWriterConnection();

    public abstract Boolean isEmpty(DataPath queue);

    public abstract Integer size(DataPath dataPath);

    /**
     *
     * @param dataPath
     * @return true if the data path locate a document
     *
     * The opposite is {@link #isContainer(DataPath)}
     */
    public abstract boolean isDocument(DataPath dataPath);



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetSystem that = (DataSetSystem) o;
        return Objects.equals(getDatabase(), that.getDatabase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDatabase());
    }

    /**
     *
     * @return the current data path
     */
    public DataPath getCurrentPath(){
        return getDataPath(".");
    }

    /**
     *
     * @param dataPath
     * @return the content of a data path in a string format
     */
    public abstract String getString(DataPath dataPath);

    public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties){
        throw new RuntimeException("A data set cannot copy its data paths. It can only read them");
    }

    public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties)
    {
        throw new RuntimeException("A data set cannot insert into its data paths. It can only read them");
    }

    /**
     *
     * @param dataPath the ancestor data path
     * @return the descendants of the data path
     */
    public abstract List<DataPath> getDescendants(DataPath dataPath);

    /**
     *
     * @param dataPath a data path container (a directory, a schema or a catalog)
     * @param glob a glob that filters the descendant data path returned
     * @return the descendant data paths representing sql tables, schema or files
     */
    public abstract List<DataPath> getDescendants(DataPath dataPath, String glob);

    /**
     *
     * @param dataPath the data path
     * @return data paths that references the data path primary key (via foreign keys)
     */
    public abstract List<DataPath> getReferences(DataPath dataPath);

    public ProcessingEngine getProcessingEngine(){
        throw new RuntimeException("A data set does not implements a processing engine. It can only read records one at a time");
    }

}
