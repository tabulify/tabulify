package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;

import java.nio.file.Path;

/**
 * A file without any structure
 *
 */
public class FsBinaryDataPath extends FsDataPathAbs implements FsDataPath {



  public FsBinaryDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore,path);
  }



  @Override
  public String getType() {
    return "binary";
  }




  @Override
  public FsBinaryFileManager getFileManager() {
    return FsBinaryFileManager.getSingeleton();
  }


  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }




}
