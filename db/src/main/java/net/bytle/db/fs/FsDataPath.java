package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A wrapper around a {@link Path} that adds the data def
 * <p>
 *
 */
public class FsDataPath extends DataPath {


  protected final Path path;
  private final FsDataStore fsDataStore;

  public FsDataPath(FsDataStore fsDataStore, Path path) {

    this.fsDataStore = fsDataStore;
    this.path = path;

  }

  protected static FsDataPath of(FsDataStore fsDataStore, Path path) {

    return new FsDataPath(fsDataStore, path);

  }



  @Override
  public DataUri getDataUri() {
    throw new RuntimeException("not implemented");
  }

  @Override
  public FsDataPath getSibling(String name) {
    Path siblingPath = path.resolveSibling(name);
    return FsTableSystem.of().getFileManager(siblingPath).createDataPath(fsDataStore,siblingPath);
  }

  @Override
  public FsDataPath getChild(String name) {
    return resolve(name);
  }

  @Override
  public FsDataPath resolve(String... names) {
    assert names.length != 0 : "The names array to resolve must not be empty";
    Path resolvedPath = null;
    for (String name : names) {
      resolvedPath = path.resolve(name);
    }
    return FsTableSystem.of().getFileManager(resolvedPath).createDataPath(fsDataStore, resolvedPath);
  }

  @Override
  public DataPath getChildAsTabular(String name) {
    Path siblingPath = path.resolve(name+".csv");
    return FsTableSystem.of().getFileManager(siblingPath).createDataPath(fsDataStore,siblingPath);
  }

  @Override
  public DataStore getDataStore() {
    return fsDataStore;
  }

  @Override
  public String getName() {
    return this.path.getFileName().toString();
  }

  @Override
  public List<String> getNames() {
    return IntStream.range(0, path.getNameCount())
      .mapToObj(i -> path.getName(i).toString())
      .collect(Collectors.toList());
  }

  public Path getNioPath() {
    return this.path;
  }

  public String getPath() {
    return this.path.toString();
  }

  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }

}
