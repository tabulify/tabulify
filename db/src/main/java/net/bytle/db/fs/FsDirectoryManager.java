package net.bytle.db.fs;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

/**
 * A directory file manager
 */
public class FsDirectoryManager extends FsFileManager {


  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    throw new RuntimeException("This is a directory, it has no content, you can't therefore ask to read its content");
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("This is a directory, it has no content, you can't therefore ask to write its content");
  }

}
