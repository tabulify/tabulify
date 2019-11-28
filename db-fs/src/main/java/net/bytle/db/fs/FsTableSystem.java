package net.bytle.db.fs;

import net.bytle.db.DatabasesStore;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.csv.CsvInsertStream;
import net.bytle.db.csv.CsvManager;
import net.bytle.db.csv.CsvSelectStream;
import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.*;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FsTableSystem extends TableSystem {


    private final Database database;
    private final FsTableSystemProvider fsTableSystemProvider;

    private FsTableSystem(FsTableSystemProvider fsTableSystemProvider, Database database) {
        assert database != null;
        this.database = database;
        this.fsTableSystemProvider = fsTableSystemProvider;
    }

    protected static FsTableSystem of(FsTableSystemProvider fsTableSystemProvider, Database database) {
        return new FsTableSystem(fsTableSystemProvider, database);
    }


    /**
     *
     * @return the default table system provider (ie the local file system)
     */
    public static FsTableSystem getDefault() {
        return FsTableSystemProvider.getDefault().getTableSystem(FsTableSystemProvider.defaultDatabase);
    }


    /**
     * @param dataUri - a data Uri
     * @return a list of file that matches the uri segments
     * <p>
     * ex: the following file uri
     * /tmp/*.md
     * will return all md file in the tmp directory
     */
    public List<DataPath> getDataPaths(DataUri dataUri) {

        List<String> pathSegments = getPathSegments(dataUri);


        // Start
        Path startPath = Paths.get(".");
        List<Path> currentMatchesPaths = new ArrayList<>();
        currentMatchesPaths.add(startPath);

        for (String s : pathSegments) {

            // Glob to regex Pattern
            String pattern = Globs.toRegexPattern(s);

            // The list where the actual matches path will be stored
            List<Path> matchesPath = new ArrayList<>();
            for (Path currentPath : currentMatchesPaths) {
                List<Path> paths = Fs.getChildrenFiles(currentPath);
                for (Path childrenPath : paths) {
                    if (childrenPath.getFileName().toString().matches(pattern)) {
                        matchesPath.add(childrenPath);
                    }
                }
            }

            if (matchesPath.size() == 0) {
                break;
            } else {
                // Recursion
                currentMatchesPaths = matchesPath;
            }

        }


        return currentMatchesPaths.stream()
                .map(s -> FsDataPath.of(this, s))
                .collect(Collectors.toList());
    }

    static List<String> getPathSegments(DataUri dataUri) {
        if (!dataUri.getDataStore().equals(DatabasesStore.LOCAL_FILE_DATABASE)) {
            throw new RuntimeException("Only a file uri from the local file system is implemented from now");
        }

        final String path = dataUri.getPath();
        List<String> pathSegments = new ArrayList<>();
        if (path.contains("/")) {
            pathSegments = Arrays.asList(path.split("/"));
        } else {
            pathSegments.add(path);
        }
        return pathSegments;
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {

        Path path = Paths.get(dataUri.getPath());
        return getDataPath(path);
    }

    @Override
    public DataPath getDataPath(String... name) {
        Path path = Paths.get(name[0], Arrays.copyOfRange(name, 1, name.length));
        return getDataPath(path);
    }

    @Override
    public Boolean exists(DataPath dataPath) {

        final FsDataPath fsDataPath = (FsDataPath) dataPath;
        return Files.exists(fsDataPath.getNioPath());

    }

    @Override
    public SelectStream getSelectStream(DataPath dataPath) {
        return CsvSelectStream.of((CsvDataPath) dataPath);
    }

    @Override
    public Database getDatabase() {
        return database;
    }



    @Override
    public boolean isContainer(DataPath dataPath) {
        return Files.isDirectory(((FsDataPath)dataPath).getNioPath());
    }

    @Override
    public void create(DataPath dataPath) {
        if (!Files.exists(((FsDataPath) dataPath).getNioPath())){
            CsvManager.create((CsvDataPath) dataPath);
        } else {
            throw new RuntimeException("The data path ("+dataPath+") already exists");
        }
    }

    @Override
    public String getProductName() {

        throw new RuntimeException("not yet implemented");
    }

    @Override
    public DataType getDataType(Integer typeCode) {

        throw new RuntimeException("not yet implemented");

    }

    @Override
    public void drop(DataPath dataPath) {
        delete(dataPath);
    }

    @Override
    public void delete(DataPath dataPath) {
        FsDataPath fsDataPath = (FsDataPath) dataPath;
        try {
            Files.delete(fsDataPath.getNioPath());
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete the file ("+fsDataPath.toString()+")",e);
        }
    }

    @Override
    public void truncate(DataPath dataPath) {
        delete(dataPath);
    }


    @Override
    public TableSystemProvider getProvider() {

        return fsTableSystemProvider;

    }

    @Override
    public InsertStream getInsertStream(DataPath dataPath) {

        final CsvDataPath fsDataPath = (CsvDataPath) dataPath;
        return CsvInsertStream.of(fsDataPath);

    }

    @Override
    public List<DataPath> getChildrenDataPath(DataPath dataPath) {

        throw new RuntimeException("not yet implemented");

    }

    /**
     * Move (for now, just a append data move, the source file is not deleted)
     * @param source - the source data path
     * @param target - the target data path
     * @param transferProperties - the properties of the transfer
     */
    @Override
    public void move(DataPath source, DataPath target, TransferProperties transferProperties) {
        FsDataPath fsSource = (FsDataPath) source;
        FsDataPath fsTarget = (FsDataPath) target;
        try {

            Files.write(
                    fsTarget.getNioPath(),
                    Files.readAllBytes(fsSource.getNioPath()),
                    StandardOpenOption.APPEND);

            // The below statement will delete the source file
            // Files.move(fsSource.getNioPath(), fsTarget.getNioPath(), StandardCopyOption.REPLACE_EXISTING);


        } catch (IOException e) {
            throw new RuntimeException("Unable to move the file", e);
        }

    }

    /**
     * @return The number of thread that can be created against the data system
     */
    @Override
    public Integer getMaxWriterConnection() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public Boolean isEmpty(DataPath queue) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public Integer size(DataPath dataPath) {
        int i=0;
        try (SelectStream selectStream = getSelectStream(dataPath)){
            while (selectStream.next()) {
                i++;
            }
        }
        return i;
    }

    @Override
    public boolean isDocument(DataPath dataPath) {
        throw new RuntimeException("not yet implemented");
    }


    @Override
    public String getString(DataPath dataPath) {
        FsDataPath fsDataPath = (FsDataPath) dataPath;
        return Fs.getFileContent(fsDataPath.getNioPath());
    }

    @Override
    public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public List<DataPath> getDescendants(DataPath dataPath) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public List<DataPath> getDescendants(DataPath dataPath, String glob) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public List<DataPath> getReferences(DataPath dataPath) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public ProcessingEngine getProcessingEngine() {
        throw new RuntimeException("Not yet implemented");
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
     */
    @Override
    public void close() {
        // No connection to the local system, no close then
    }


    public FsDataPath getDataPath(Path path) {
        return new CsvDataPath(this,path);
    }
}
