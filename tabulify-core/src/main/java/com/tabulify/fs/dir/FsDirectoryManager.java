package com.tabulify.fs.dir;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsFileManager;
import net.bytle.type.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A directory file manager
 */
public class FsDirectoryManager implements FsFileManager {

  public static FsDirectoryManager getSingleton() {
    return new FsDirectoryManager();
  }

  @Override
  public FsDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {
    return new FsDirectoryDataPath(fsConnection, relativePath);
  }

  @Override
  public void create(FsDataPath fsDataPath) {
    try {
      Files.createDirectory(fsDataPath.getAbsoluteNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
