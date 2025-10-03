package com.tabulify.yaml;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class YamlManagerProvider extends FsFileManagerProvider {

  static private YamlManagerFs yamlManager;

  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.getSubType().equals(MediaTypes.TEXT_YAML.getSubType());

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (yamlManager == null) {
      yamlManager = new YamlManagerFs();
    }
    return yamlManager;
  }

}
