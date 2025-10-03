package com.tabulify.fs;

import com.tabulify.resource.ManifestResourceProvider;
import com.tabulify.spi.DataPath;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public interface FsFileManager {


  /**
   * @param fsConnection - the connection where the file is located
   * @param relativePath - the path - the relative path in the connection based on {@link FsConnection#getCurrentDataPath()}
   * @param mediaType    - the media type - the type of file
   *                     Media Type is there as a bonus to no make another call in case of remote file                      system as the media type should have been already instantiated to get the {@link FsDataSystem#getFileManager(MediaType)}
   * @return a data path.
   * It's generic because the {@link ManifestResourceProvider} can create any type of Data Path with a {@link com.tabulify.conf.ManifestDocument resource manifest}
   * It can create a {@link FsDataPath} but also SqlDataPath
   */
  DataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType);

  /**
   * @param fsDataPath - create the data path on the disk
   */
  void create(FsDataPath fsDataPath);


  /**
   * Create a runtime data path from a data path
   *
   * @param executionConnection - the execution connection
   * @param executableDataPath  - the executable data path
   */
  default FsDataPath createRuntimeDataPath(FsConnection executionConnection, FsDataPath executableDataPath) {
    throw new UnsupportedOperationException("The resource " + executableDataPath + " is not yet implemented as an executable");
  }

}
