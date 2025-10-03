package com.tabulify.fs.textfile;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

public class FsTextManager extends FsBinaryFileManager implements FsFileManager {


  private static FsTextManager fsTextManager;

  public static FsBinaryFileManager getSingeleton() {
    if (fsTextManager == null) {
      fsTextManager = new FsTextManager();
    }
    return fsTextManager;
  }

  @Override
  public FsTextDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    return new FsTextDataPath(fsConnection, relativePath, MediaTypes.TEXT_PLAIN);

  }






}
