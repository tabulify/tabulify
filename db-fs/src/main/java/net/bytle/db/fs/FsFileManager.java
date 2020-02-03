package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FsFileManager {

  private final FsTableSystem fsTableSystem;

  public FsFileManager(FsTableSystem fsTableSystem) {
    this.fsTableSystem = fsTableSystem;
  }

  public static FsFileManager of(FsTableSystem fsTableSystem) {
    return new FsFileManager(fsTableSystem);
  }

  public void create(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path path = fsDataPath.getNioPath();
    try {
      if (Files.isDirectory(path)) {
        Files.createDirectory(path);
      } else {
        Files.createFile(path);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public FsDataPath getDataPath(Path path) {
    return new FsDataPath(fsTableSystem, path);
  }

  protected FsTableSystem getFsTableSystem() {
    return fsTableSystem;
  }

}
