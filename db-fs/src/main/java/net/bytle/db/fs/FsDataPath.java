package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A wrapper around a {@link Path} that adds the data def
 * <p>
 * If you want to use a local file, use {@link FsTableSystem#getDefault() the default file system} to instantiate
 * a data path with the function {@link FsTableSystem#getDataPath(Path)}
 */
public class FsDataPath extends DataPath {


  protected final Path path;
  private final FsTableSystem tableSystem;

  protected FsDataPath(FsTableSystem fsTableSystem, Path path) {

    this.tableSystem = fsTableSystem;
    this.path = path;

  }

  protected static FsDataPath of(FsTableSystem fsTableSystem, Path path) {

    return new FsDataPath(fsTableSystem, path);

  }


  @Override
  public FsTableSystem getDataSystem() {
    return tableSystem;
  }


  @Override
  public DataUri getDataUri() {
    throw new RuntimeException("not implemented");
  }

  @Override
  public FsDataPath getSibling(String name) {
    Path siblingPath = path.resolveSibling(name);
    return getDataSystem().getFileManager(siblingPath).getDataPath(siblingPath);
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
    return getDataSystem().getFileManager(resolvedPath).getDataPath(resolvedPath);
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
  public List getSelectStreamDependencies() {
    throw new RuntimeException("No select stream dependencies here");
  }
}
