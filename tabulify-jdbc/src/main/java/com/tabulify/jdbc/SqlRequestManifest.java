package com.tabulify.jdbc;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.resource.ManifestKindManager;
import com.tabulify.resource.ManifestKindMediaType;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

public class SqlRequestManifest extends FsFileManagerProvider {

  final static public MediaType SQL_REQUEST_YAML = new ManifestKindMediaType(KeyNormalizer.createSafe("sql-request"));


  static ManifestKindManager manifestKindManager = ManifestKindManager.builder()
    .setTargetMediaType(SqlMediaType.REQUEST)
    .build();


  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.equals(SQL_REQUEST_YAML);

  }


  @Override
  public ManifestKindManager getFsFileManager() {
    return manifestKindManager;
  }


}
