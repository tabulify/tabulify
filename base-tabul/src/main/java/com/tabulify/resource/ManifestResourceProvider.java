package com.tabulify.resource;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

public class ManifestResourceProvider extends FsFileManagerProvider {

  final static MediaType MEDIA_TYPE = new ManifestKindMediaType(KeyNormalizer.createSafe("resource"));

  static final ManifestResourceManager MANIFEST_RESOURCE_MANAGER = new ManifestResourceManager();

  @Override
  public Boolean accept(MediaType mediaType) {

    return MediaTypes.equals(mediaType, MEDIA_TYPE);

  }


  @Override
  public ManifestResourceManager getFsFileManager() {
    return MANIFEST_RESOURCE_MANAGER;
  }

}
