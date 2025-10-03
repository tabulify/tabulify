package com.tabulify.zip.datapath;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.zip.api.ArchiveMediaType;
import net.bytle.type.MediaType;

import java.nio.file.Path;


public class ArchiveManager extends FsBinaryFileManager {

  protected static ArchiveManager archiveManager = new ArchiveManager();


  @Override
  public ArchiveDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {
    return new ArchiveDataPath(fsConnection, relativePath, ArchiveMediaType.castSafe(mediaType));
  }

}
