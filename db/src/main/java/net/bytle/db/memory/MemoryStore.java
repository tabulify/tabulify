package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.cli.Log;
import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.Streams;
import net.bytle.db.uri.DataUri;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MemoryStore extends TableSystem {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    static private Map<DataPath, List<List<Object>> > tableValues = new HashMap<>();
    private static MemoryStore staticMemoryStore;
    private final MemorySystemProvider memoryStoreProvider;
    private Database database;

    public MemoryStore(MemorySystemProvider memorySystemProvider) {
        this.memoryStoreProvider = memorySystemProvider;
    }

    public static List<List<Object>>  get(DataPath memoryTable) {
        return tableValues.computeIfAbsent(memoryTable, k -> new ArrayList<>());
    }

    public static MemoryStore of(MemorySystemProvider memorySystemProvider) {
        if (staticMemoryStore == null){
            staticMemoryStore = new MemoryStore(memorySystemProvider);
        }
        return staticMemoryStore;
    }

    public void delete(DataPath memoryTable) {
        Object values = tableValues.remove(memoryTable);
        if (values == null) {
            LOGGER.warning("The table (" + memoryTable + ") had no values. Nothing removed.");
        }
    }

    public void drop(DataPath memoryTable) {
        delete(memoryTable);
    }

    public  void truncate(DataPath memoryTable) {
        tableValues.put(memoryTable,new ArrayList<>());
    }

    @Override
    public <T> T getMin(ColumnDef<T> columnDef) {

        throw new RuntimeException("Not Implemented");

    }

    @Override
    public void dropForeignKey(ForeignKeyDef foreignKeyDef) {
        // Nothing to do
    }

    @Override
    public SelectStream getSelectStream(String query) {

        throw new RuntimeException("Getting a stream from a query is not supported");

    }

    @Override
    public TableSystemProvider getProvider() {
        return this.memoryStoreProvider;
    }


    public InsertStream getInsertStream(DataPath memoryTable) {
        return ListInsertStream.of(memoryTable);
    }

    @Override
    public List<DataPath> getChildrenDataPath(DataPath dataPath) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public DataPath move(DataPath source, DataPath target) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {
        this.database = dataUri.getDataStore();
        return getDataPath(dataUri.getPathSegments().toArray(new String[0]));
    }

    @Override
    public DataPath getDataPath(String... names) {
        return MemoryDataPath.of(this,names);
    }

    @Override
    public Boolean exists(DataPath dataPath) {
        return tableValues.containsKey(dataPath);
    }

    public SelectStream getSelectStream(DataPath memoryTable) {
        return ListSelectStream.of(memoryTable);
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    @Override
    public <T> T getMax(ColumnDef<T> columnDef) {

        throw new RuntimeException("Not implemented");

    }

    @Override
    public boolean isContainer(DataPath dataPath) {

        return false;

    }

    @Override
    public DataPath create(DataPath dataPath) {
        List<List<Object>> memoryTabularData = new ArrayList<>();
        tableValues.put(dataPath,memoryTabularData);
        return dataPath;
    }

    @Override
    public String getProductName() {
        return "memory";
    }

    @Override
    public DataType getDataType(Integer typeCode) {
        return null;
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     *
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     *
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     *
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     *
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close()  {

    }
}
