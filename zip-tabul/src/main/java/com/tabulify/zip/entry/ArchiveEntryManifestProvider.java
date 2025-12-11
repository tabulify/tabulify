package com.tabulify.zip.entry;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.resource.ManifestKindManager;
import com.tabulify.resource.ManifestKindMediaType;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

public class ArchiveEntryManifestProvider extends FsFileManagerProvider {

  /**
   * Yaml Media type
   */
  final static public MediaType ARCHIVE_ENTRY_MANIFEST = new ManifestKindMediaType(KeyNormalizer.createSafe("archive-entry"));

  static ManifestKindManager manifestKindManager = ManifestKindManager
    .builder()
    .setTargetMediaType(ArchiveEntryProvider.ARCHIVE_ENTRY)
    .build();


  @Override
  public Boolean accept(MediaType mediaType) {

    return MediaTypes.equals(mediaType, ARCHIVE_ENTRY_MANIFEST);

  }


  @Override
  public ManifestKindManager getFsFileManager() {
    return manifestKindManager;
  }


}
