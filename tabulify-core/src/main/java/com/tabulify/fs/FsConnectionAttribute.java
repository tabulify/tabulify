package com.tabulify.fs;

import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class FsConnectionAttribute {

  /**
   * When we move tabular data into a file system,
   * the below extension (tabular format) for the file is used
   */
  public static final MediaType FS_DEFAULT_TABULAR_MEDIA_TYPE = MediaTypes.TEXT_CSV;

}
