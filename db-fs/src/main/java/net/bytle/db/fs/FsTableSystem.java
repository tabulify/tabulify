package net.bytle.db.fs;

import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.csv.CsvInsertStream;
import net.bytle.db.csv.CsvManager;
import net.bytle.db.csv.CsvSelectStream;
import net.bytle.db.database.DataStore;
import net.bytle.db.database.FileDataStore;
import net.bytle.db.html.HtmlDataPath;
import net.bytle.db.json.JsonDataPath;
import net.bytle.db.json.JsonSelectStream;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper around a {@link java.nio.file.FileSystem}
 */
public class FsTableSystem extends TableSystem {


  private final FileDataStore fileDataStore;
  private final FsTableSystemProvider fsTableSystemProvider;
  private final FileSystem fileSystem;

  private FsTableSystem(FsTableSystemProvider fsTableSystemProvider, FileDataStore fileDataStore) {
    assert fileDataStore != null;
    this.fileDataStore = fileDataStore;
    this.fsTableSystemProvider = fsTableSystemProvider;
    this.fileSystem = Paths.get(this.fileDataStore.getUri()).getFileSystem();
  }

  protected static FsTableSystem of(FsTableSystemProvider fsTableSystemProvider, FileDataStore database) {
    return new FsTableSystem(fsTableSystemProvider, database);
  }


  /**
   * @return the default table system provider (ie the local file system)
   */
  public static FsTableSystem getDefault() {
    FileDataStore defaultDatabase =  FileDataStore.of(Paths.get("."));
    return FsTableSystemProvider.getDefault().getTableSystem(defaultDatabase);
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

    String[] pathSegments = dataUri.getPath().split(this.fileSystem.getSeparator());

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


  @Override
  public DataPath getDataPath(DataUri dataUri) {

    Path path = Paths.get(dataUri.getPath());
    return getDataPath(path);
  }

  @Override
  public FsDataPath getDataPath(String... names) {

    return getCurrentPath().resolve(names);

  }

  @Override
  public Boolean exists(DataPath dataPath) {

    final FsDataPath fsDataPath = (FsDataPath) dataPath;
    return Files.exists(fsDataPath.getNioPath());

  }

  @Override
  public SelectStream getSelectStream(DataPath dataPath) {
    // TODO: We need to get it from the path ?
    if (CsvDataPath.class.equals(dataPath.getClass())) {
      return CsvSelectStream.of((CsvDataPath) dataPath);
    } else if (JsonDataPath.class.equals(dataPath.getClass())){
      return JsonSelectStream.of((JsonDataPath) dataPath);
    } else {
      throw new RuntimeException("We cannot give a select stream because this data path type ("+dataPath.getClass()+") has no known select stream.");
    }

  }

  @Override
  public DataStore getDataStore() {
    return this.fileDataStore;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return Files.isDirectory(((FsDataPath) dataPath).getNioPath());
  }

  @Override
  public void create(DataPath dataPath) {
    Path nioPath = ((FsDataPath) dataPath).getNioPath();
    if (!Files.exists(nioPath)) {
      CsvManager.create((CsvDataPath) dataPath);
    } else {
      throw new RuntimeException("The data path (" + nioPath + ") already exists");
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
      throw new RuntimeException("Unable to delete the file (" + fsDataPath.toString() + ")", e);
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
   *
   * @param source             - the source data path
   * @param target             - the target data path
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
    int i = 0;
    try (SelectStream selectStream = getSelectStream(dataPath)) {
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
  public FsDataPath getCurrentPath() {
    Path currentPath = Paths.get(this.fileDataStore.getUri());
    return new FsDataPath(this, currentPath);
  }


  @Override
  public String getString(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    return Fs.getFileContent(fsDataPath.getNioPath());
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
    FsDataPath fsSource = (FsDataPath) source;
    FsDataPath fsTarget = (FsDataPath) target;
    TransferListener transferListener = TransferListener.of()
      .startTimer();
    try {
      Files.copy(fsSource.getNioPath(), fsTarget.getNioPath());
    } catch (IOException e) {
      transferListener.addException(e);
      throw new RuntimeException(e);
    }
    return transferListener.stopTimer();
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

  @Override
  public DataPath getChildOf(DataPath dataPath, String part) {
    assert isContainer(dataPath) : "You cannot get a child from the document (" + dataPath + ")";
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path childPath = fsDataPath.getNioPath().resolve(part);
    return getDataPath(childPath);
  }

  @Override
  protected DataPath getRootPath() {
    try {
      URI connectionUri = this.fileDataStore.getUri();
      // Port may be not given
      Integer port = connectionUri.getPort();
      // A rootUri has no path
      URI rootUri = new URI(
        connectionUri.getScheme(),
        connectionUri.getUserInfo(),
        connectionUri.getHost(),
        connectionUri.getPort(), // You can pass -1
        "",
        connectionUri.getQuery(),
        connectionUri.getFragment());
      return new FsDataPath(this, Paths.get(rootUri));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DataPath getChildDataPath(URI uri) {
    return getDataPath(Paths.get(uri));
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

    switch (path.toUri().getScheme()) {
      case "file":
        String extension = Fs.getExtension(path.toString()).toLowerCase();
        switch (extension) {
          case "csv":
            return new CsvDataPath(this, path);
          case "jsonl":
          case "json":
            return new JsonDataPath(this, path);
          default:
            return new FsDataPath(this, path);
        }
      case "https":
      case "http":
        return new HtmlDataPath(this, path);
      default:
        return new FsDataPath(this, path);
    }

  }
}
