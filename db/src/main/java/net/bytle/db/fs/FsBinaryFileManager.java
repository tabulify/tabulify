package net.bytle.db.fs;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.io.IOException;
import java.nio.file.Files;
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
public class FsBinaryFileManager {



  public FsBinaryFileManager() {

  }


  public static FsBinaryFileManager getSingeleton() {
   return new FsBinaryFileManager();
  }


  public SelectStream getSelectStream(FsDataPath fsDataPath){
    throw new RuntimeException("This file ("+ fsDataPath + ") has no known structure and/or manager and therefore can't return a select stream");
  }

  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    throw new RuntimeException("This file ("+ fsDataPath + ") has no known structure and/or manager and therefore can't return an insert stream");
  }

  public FsDataPath createDataPath(FsDataStore fsDataStore, Path path) {
    throw new RuntimeException("This file ("+ path + ") has no known structure and/or manager and therefore can't return an insert stream");
  }

  /**
   * Create the file
   * For instance, if it's a CSV, you may need to create the headers
   * @param fsDataPath
   */
  public void create(FsDataPath fsDataPath){
    try {
      Files.createFile(fsDataPath.getNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
