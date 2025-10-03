package com.tabulify.csv;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.resource.ManifestKindManager;
import com.tabulify.resource.ManifestKindMediaType;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

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
