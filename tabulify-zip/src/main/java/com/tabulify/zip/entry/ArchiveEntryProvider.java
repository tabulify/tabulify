package com.tabulify.zip.entry;


import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypeAbs;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

public class ArchiveEntryProvider extends FsFileManagerProvider {

  protected static ArchiveEntryManager archiveEntryManager = new ArchiveEntryProvider.ArchiveEntryManager();

  /**
   * Java Media type
   */
  final static public MediaType ARCHIVE_ENTRY = new MediaTypeAbs() {
    @Override
    public String getSubType() {
      return "vnd.tabulify." + ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST.getKind();
    }

    @Override
    public String getType() {
      return "application";
    }

    @Override
    public KeyNormalizer getKind() {
      return ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST.getKind();
    }
  };

  @Override
  public Boolean accept(MediaType mediaType) {

    return MediaTypes.equals(mediaType, ARCHIVE_ENTRY);

  }


  @Override
  public FsBinaryFileManager getFsFileManager() {
    return archiveEntryManager;
  }


  public static class ArchiveEntryManager extends FsBinaryFileManager {


    @Override
    public ArchiveEntryDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {
      return new ArchiveEntryDataPath(fsConnection, relativePath, mediaType);
    }

    @Override
    public FsDataPath createRuntimeDataPath(FsConnection executionConnection, FsDataPath executableDataPath) {
      return new ArchiveEntryDataPathRuntime(executionConnection, (ArchiveEntryDataPath) executableDataPath);
    }

  }

}
