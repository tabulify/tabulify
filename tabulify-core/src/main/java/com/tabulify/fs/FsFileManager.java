package com.tabulify.fs;

import java.nio.file.Path;

public interface FsFileManager {



  FsDataPath createDataPath(FsConnection fsConnection, Path path);

  void create(FsDataPath fsDataPath);


}
