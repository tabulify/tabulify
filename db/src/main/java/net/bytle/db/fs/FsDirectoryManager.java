package net.bytle.db.fs;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

/**
 * A directory file manager
 */
public class FsDirectoryManager extends FsBinaryFileManager {

  public static FsDirectoryManager getSingeleton() {
    return new FsDirectoryManager();
  }

  @Override
  public FsDataPath createDataPath(FsDataStore fsDataStore, Path path) {
    return new FsDirectoryDataPath(fsDataStore,path);
  }

  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    throw new RuntimeException("This is a directory, it has no content, you can't therefore ask to read its content");
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("This is a directory, it has no content, you can't therefore ask to write its content");
  }

}
