package com.tabulify.resource;

import com.tabulify.fs.FsFileManagerProvider;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

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
