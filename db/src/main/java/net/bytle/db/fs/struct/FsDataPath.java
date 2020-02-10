package net.bytle.db.fs.struct;

import net.bytle.db.fs.FsTableSystem;
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
  private final FsTableSystem tableSystem;

  public FsDataPath(FsTableSystem fsTableSystem, Path path) {

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
    return getDataSystem().getFileManager(siblingPath).createDataPath(getDataSystem(),siblingPath);
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
    return getDataSystem().getFileManager(resolvedPath).createDataPath(getDataSystem(), resolvedPath);
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
    throw new RuntimeException("No select stream dependencies here");
  }
}
