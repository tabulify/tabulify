package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.uri.DataUri;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 *
 */
public class FsRawDataPath extends DataPathAbs implements FsDataPath {


  protected final Path path;
  private final FsDataStore fsDataStore;

  public FsRawDataPath(FsDataStore fsDataStore, Path path) {

    this.fsDataStore = fsDataStore;
    this.path = path;

  }

  protected static FsRawDataPath of(FsDataStore fsDataStore, Path path) {

    return new FsRawDataPath(fsDataStore, path);

  }


  @Override
  public DataUri getDataUri() {
    throw new RuntimeException("not implemented");
  }

  @Override
  public FsRawDataPath getSibling(String name) {
    Path siblingPath = path.resolveSibling(name);
    return (FsRawDataPath) FsTableSystem.of().getFileManager(siblingPath).createDataPath(fsDataStore, siblingPath);
  }

  @Override
  public FsRawDataPath getChild(String name) {
    return resolve(name);
  }

  @Override
  public FsRawDataPath resolve(String... names) {
    assert names.length != 0 : "The names array to resolve must not be empty";
    Path resolvedPath = null;
    for (String name : names) {
      resolvedPath = path.resolve(name);
    }
    return (FsRawDataPath) FsTableSystem.of().getFileManager(resolvedPath).createDataPath(fsDataStore, resolvedPath);
  }

  @Override
  public FsDataPath getChildAsTabular(String name) {
    Path siblingPath = path.resolve(name + ".csv");
    return FsTableSystem.of().getFileManager(siblingPath).createDataPath(fsDataStore, siblingPath);
  }

  @Override
  public String getType() {
    return "file";
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

  @Override
  public Path getNioPath() {
    return this.path;
  }

  @Override
  public String getPath() {

    return this.path.toString();

  }



  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }


}
