package net.bytle.db.json;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsTableSystem;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class JsonManager extends FsFileManager {



  @Override
  public FsDataPath createDataPath(FsTableSystem fsTableSystem, Path path) {
    return new JsonDataPath(fsTableSystem, path);
  }


  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    return new JsonSelectStream((JsonDataPath) fsDataPath);
  }


}
