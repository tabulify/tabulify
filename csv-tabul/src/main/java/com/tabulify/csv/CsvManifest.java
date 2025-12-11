package com.tabulify.csv;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.resource.ManifestKindManager;
import com.tabulify.resource.ManifestKindMediaType;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

public class CsvManifest extends FsFileManagerProvider {

  final static public MediaType MANIFEST_MEDIA_TYPE = new ManifestKindMediaType(KeyNormalizer.createSafe("csv"));


  static ManifestKindManager manifestKindManager = ManifestKindManager.builder()
    .setTargetMediaType(MediaTypes.TEXT_CSV)
    .build();


  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.equals(MANIFEST_MEDIA_TYPE);

  }


  @Override
  public ManifestKindManager getFsFileManager() {
    return manifestKindManager;
  }


}
