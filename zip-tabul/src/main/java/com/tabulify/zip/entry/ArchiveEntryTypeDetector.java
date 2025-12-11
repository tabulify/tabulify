package com.tabulify.zip.entry;


import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class ArchiveEntryTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST.getExtension())) {
      return null;
    }
    return ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST.toString();

  }

}
