package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;

import java.nio.file.Path;

/**
 * A basic directory manager
 *
 */
public class FsDirectoryDataPath extends FsDataPathAbs implements FsDataPath {



  public FsDirectoryDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore,path);
  }



  @Override
  public String getType() {
    return "directory";
  }




  @Override
  public FsBinaryFileManager getFileManager() {
    return FsDirectoryManager.getSingeleton();
  }


  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }




}
