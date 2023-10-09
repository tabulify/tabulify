package net.bytle.db.fs.dir;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsFileManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A directory file manager
 */
public class FsDirectoryManager implements FsFileManager {

  public static FsDirectoryManager getSingleton() {
    return new FsDirectoryManager();
  }

  @Override
  public FsDataPath createDataPath(FsConnection fsConnection, Path path) {
    return new FsDirectoryDataPath(fsConnection, path);
  }

  @Override
  public void create(FsDataPath fsDataPath) {
    try {
      Files.createDirectory(fsDataPath.getAbsoluteNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
