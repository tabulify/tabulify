package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;

import java.nio.file.Path;

public abstract class FsFileManager {

  private final FsTableSystem fsTableSystem;

  public FsFileManager(FsTableSystem fsTableSystem) {
    this.fsTableSystem = fsTableSystem;
  }

  public abstract void create(DataPath dataPath);

  public abstract FsDataPath getDataPath(Path path);

  protected FsTableSystem getFsTableSystem() {
    return fsTableSystem;
  }
}
