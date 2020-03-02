package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.gen.GenDataPath;
import net.bytle.db.gen.GenSelectStream;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class GenFsManager extends FsBinaryFileManager {


  @Override
  public GenFsDataPath createDataPath(FsDataStore fsDataStore, Path path) {

    return new GenFsDataPath(fsDataStore, path);

  }


  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    return new GenSelectStream((GenDataPath) fsDataPath);
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("A generator data file generated only data. You can't therefore insert in a generator data path");
  }

}
