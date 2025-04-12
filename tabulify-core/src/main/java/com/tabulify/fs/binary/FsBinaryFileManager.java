package com.tabulify.fs.binary;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.FsFileManagerProvider;
import net.bytle.fs.Fs;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

/**
 * A file manager is responsible for:
 * * the instantiation of a file with a structure
 * * the retrieving of a select and insert stream
 * <p>
 * This file manager is the default one when the content is not known
 * <p>
 * A file manager for a defined type (Csv, ...) can be defined via the {@link FsFileManagerProvider}
 */
public class FsBinaryFileManager implements FsFileManager {


  public FsBinaryFileManager() {

  }


  public static FsBinaryFileManager getSingleton() {
    return new FsBinaryFileManager();
  }


  @Override
  public FsDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new FsBinaryDataPath(fsConnection, path, MediaTypes.BINARY_FILE);

  }

  /**
   * Create the file
   * For instance, if it's a CSV, you may need to create the headers
   *
   * @param fsDataPath -  the data path to create
   */
  @Override
  public void create(FsDataPath fsDataPath) {
    Fs.createEmptyFile(fsDataPath.getAbsoluteNioPath());
  }


}
