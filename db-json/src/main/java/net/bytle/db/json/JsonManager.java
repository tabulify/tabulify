package net.bytle.db.json;

import net.bytle.db.fs.*;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class JsonManager extends FsBinaryFileManager implements FsFileManager {




  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    return new JsonSelectStream((JsonDataPath) fsDataPath);
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public FsDataPath createDataPath(FsDataStore fsDataStore, Path path) {
    return new JsonDataPath(fsDataStore, path);
  }


}
