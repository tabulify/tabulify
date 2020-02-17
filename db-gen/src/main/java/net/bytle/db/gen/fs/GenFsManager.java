package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsRawDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsFileManager;
import net.bytle.db.gen.GenSelectStream;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class GenFsManager extends FsFileManager {


  @Override
  public GenFsDataPath createDataPath(FsDataStore fsDataStore, Path path) {

    return new GenFsDataPath(fsDataStore, path);

  }




  @Override
  public SelectStream getSelectStream(FsRawDataPath fsDataPath) {
    return new GenSelectStream(fsDataPath);
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("A generator data file generated only data. You can't therefore insert in a generator data path");
  }
}
