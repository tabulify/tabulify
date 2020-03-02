package net.bytle.db.json;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsBinaryDataPath;
import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsDataSystem;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class JsonManager extends FsBinaryFileManager {



  @Override
  public FsDataPath createDataPath(FsDataSystem fsTableSystem, Path path) {
    return new JsonDataPath(fsTableSystem, path);
  }


  @Override
  public SelectStream getSelectStream(FsBinaryDataPath fsDataPath) {
    return new JsonSelectStream((JsonDataPath) fsDataPath);
  }


}
