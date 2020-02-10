package net.bytle.db.fs.struct;

import net.bytle.db.fs.FsTableSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

/**
 * A wrapper around a FsDataPath to give the implementation specific structure function
 */
public class FsFileManager  {



  public FsFileManager() {

  }

  public static FsFileManager of() {
   return new FsFileManager();
  }


  public SelectStream getSelectStream(FsDataPath fsDataPath){
    throw new RuntimeException("This file ("+ fsDataPath + ") has no known structure and/or manager and therefore can't return a select stream");
  }

  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("This file ("+ fsDataPath + ") has no known structure and/or manager and therefore can't return an insert stream");
  }

  public FsDataPath createDataPath(FsTableSystem fsTableSystem, Path path) {
    return FsDataPath.of(fsTableSystem, path);
  }

}
