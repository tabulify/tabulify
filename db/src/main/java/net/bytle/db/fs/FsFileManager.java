package net.bytle.db.fs;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

/**
 * A file manager is responsible for:
 *   * the instantiation of a file with a structure
 *   * the retrieving of a select and insert stream
 *
 * This file manager is the default one when the content is not known
 *
 * A file manager for a defined type (Csv, ...) can be defined via the {@link FsFileManagerProvider}
 *
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
