package net.bytle.db.fs;

import net.bytle.db.DatabasesStore;
import net.bytle.db.csv.CsvSelectStream;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FsTableSystem extends TableSystem {


    private final Database database;

    private FsTableSystem(Database database) {
        assert database!=null;
        this.database = database;
    }

    protected static TableSystem of(Database database) {
        return new FsTableSystem(database);
    }

    public static FsTableSystem of() {
        return new FsTableSystem(Databases.of("FsDefault"));
    }


    /**
     *
     * @param dataUri
     * @return a list of file that matches the uri segments
     *
     * ex: the following file uri
     *   /tmp/*.md
     * will return all md file in the tmp directory
     *
     */
    public List<DataPath> getDataPaths(DataUri dataUri) {

        List<String> pathSegments = getPathSegments(dataUri);


        // Start
        Path startPath = Paths.get(".");
        List<Path> currentMatchesPaths = new ArrayList<>();
        currentMatchesPaths.add(startPath);

        for (String s: pathSegments){

            // Glob to regex Pattern
            String pattern = Globs.toRegexPattern(s);

            // The list where the actual matches path will be stored
            List<Path> matchesPath = new ArrayList<>();
            for (Path currentPath: currentMatchesPaths) {
                List<Path> paths = Fs.getChildrenFiles(currentPath);
                for (Path childrenPath : paths) {
                    if (childrenPath.getFileName().toString().matches(pattern)) {
                        matchesPath.add(childrenPath);
                    }
                }
            }

            if (matchesPath.size()==0){
                break;
            } else {
                // Recursion
                currentMatchesPaths = matchesPath;
            }

        }


        return currentMatchesPaths.stream()
                .map(s->FsDataPath.of(this,s))
                .collect(Collectors.toList());
    }

    static List<String> getPathSegments(DataUri dataUri) {
        if (!dataUri.getDataStore().equals(DatabasesStore.LOCAL_FILE_DATABASE)){
            throw new RuntimeException("Only a file uri from the local file system is implemented from now");
        }

        final String path = dataUri.getPath();
        List<String> pathSegments = new ArrayList<>();
        if (path.contains("/")){
            pathSegments = Arrays.asList(path.split("/"));
        } else {
            pathSegments.add(path);
        }
        return pathSegments;
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {

        final List<String> pathSegments = getPathSegments(dataUri);
        return getDataPath(pathSegments.toArray(new String[0]));
    }

    @Override
    public DataPath getDataPath(String... name) {
        Path path = Paths.get(name[0],Arrays.copyOfRange(name,1,name.length));
        return FsDataPath.of(this,path);
    }

    @Override
    public Boolean exists(DataPath dataPath) {

        final FsDataPath fsDataPath = (FsDataPath) dataPath;
        return Files.exists(fsDataPath.getPath());

    }

    @Override
    public SelectStream getSelectStream(DataPath dataPath) {
        final FsDataPath fsDataPath = (FsDataPath) dataPath;
        return CsvSelectStream.of(fsDataPath);
    }

    @Override
    public Database getDatabase() {
        return null;
    }

    @Override
    public <T> T getMax(ColumnDef<T> columnDef) {
        return null;
    }

    @Override
    public boolean isContainer(DataPath dataPath) {
        return false;
    }

    @Override
    public DataPath create(DataPath dataPath) {
        return null;
    }

    @Override
    public String getProductName() {
        return null;
    }

    @Override
    public DataType getDataType(Integer typeCode) {
        return null;
    }

    @Override
    public void drop(DataPath dataPath) {

    }

    @Override
    public void delete(DataPath dataPath) {

    }

    @Override
    public void truncate(DataPath dataPath) {

    }

    @Override
    public <T> T getMin(ColumnDef<T> columnDef) {
        return null;
    }

    @Override
    public void dropForeignKey(ForeignKeyDef foreignKeyDef) {

    }

    @Override
    public TableSystemProvider getProvider() {
        return null;
    }

    @Override
    public InsertStream getInsertStream(DataPath dataPath) {
        return null;
    }

    @Override
    public List<DataPath> getChildrenDataPath(DataPath dataPath) {
        return null;
    }

    @Override
    public DataPath move(DataPath source, DataPath target) {
        return null;
    }

    /**
     * @return The number of thread that can be created against the data system
     */
    @Override
    public Integer getMaxWriterConnection() {
        return null;
    }

    @Override
    public Boolean isEmpty(DataPath queue) {
        return null;
    }

    @Override
    public Integer size(DataPath dataPath) {
        return null;
    }

    @Override
    public boolean isDataUnit(DataPath dataPath) {
        return false;
    }

    @Override
    public DataPath getQuery(String query) {
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
     */
    @Override
    public void close() {
        // No connection to the local system, no close then
    }
}
