package com.tabulify.fs.runtime;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.resource.ManifestKindManager;
import com.tabulify.resource.ManifestKindMediaType;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import static com.tabulify.fs.runtime.FsCommandMediaType.COMMAND_MEDIA_TYPE;

public class FsCommandManifestProvider extends FsFileManagerProvider {

  /**
   * Yaml Media type
   */
  final static public MediaType COMMAND_MANIFEST_TYPE = new ManifestKindMediaType(KeyNormalizer.createSafe("command"));

  static ManifestKindManager manifestKindManager = ManifestKindManager
    .builder()
    .setTargetMediaType(COMMAND_MEDIA_TYPE)
    .build();


  @Override
  public Boolean accept(MediaType mediaType) {

    return MediaTypes.equals(mediaType, COMMAND_MANIFEST_TYPE);

  }


  @Override
  public ManifestKindManager getFsFileManager() {
    return manifestKindManager;
  }


}
