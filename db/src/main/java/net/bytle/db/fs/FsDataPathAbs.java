package net.bytle.db.fs;

import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.uri.DataUri;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class FsDataPathAbs extends DataPathAbs implements FsDataPath {

  private final FsDataStore fsDataStore;
  private final Path path;

  public FsDataPathAbs(FsDataStore fsDataStore, Path path) {
    this.fsDataStore = fsDataStore;
    this.path = path;
  }

  @Override
  public FsDataPath getSibling(String name) {
    Path siblingPath = path.resolveSibling(name);
    return this.getDataStore().getDataSystem().getFileManager(siblingPath).createDataPath(fsDataStore, siblingPath);
  }

  @Override
  public FsDataPath resolve(String... names) {
    assert names.length != 0 : "The names array to resolve must not be empty";
    Path resolvedPath = null;
    for (String name : names) {
      resolvedPath = path.resolve(name);
    }
    return this.getDataStore().getDataSystem().getFileManager(resolvedPath).createDataPath(fsDataStore, resolvedPath);
  }

  @Override
  public FsDataPath getChildAsTabular(String name) {
    Path siblingPath = path.resolve(name + ".csv");
    return this.getDataStore().getDataSystem().getFileManager(siblingPath).createDataPath(fsDataStore, siblingPath);
  }

  @Override
  public FsDataPath getChild(String name) {
    return resolve(name);
  }

  @Override
  public String getPath() {

    return this.path.toString();

  }

  @Override
  public Path getNioPath() {
    return this.path;
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
  public FsDataStore getDataStore() {
    return fsDataStore;
  }

  @Override
  public DataUri getDataUri() {
    throw new RuntimeException("Not implemented");
  }

}
