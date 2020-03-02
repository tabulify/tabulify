package net.bytle.db.fs;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public interface FsFileManager {
  SelectStream getSelectStream(FsDataPath fsDataPath);

  InsertStream getInsertStream(FsDataPath fsDataPath);

  FsDataPath createDataPath(FsDataStore fsDataStore, Path path);

  void create(FsDataPath fsDataPath);
}
