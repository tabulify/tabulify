package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;

import java.nio.file.Path;

/**
 * A wrapper around a {@link Path} that adds the data def
 */
public interface FsDataPath extends DataPath {



  Path getNioPath();

  FsFileManager getFileManager();

  @Override
  FsDataStore getDataStore();

}
