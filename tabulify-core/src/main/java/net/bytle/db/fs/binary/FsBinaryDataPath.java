package net.bytle.db.fs.binary;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataPathAbs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.type.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The base data path -
 * - A file without any structure (a binary)
 */
public class FsBinaryDataPath extends FsDataPathAbs implements FsDataPath {


  public FsBinaryDataPath(FsConnection fsConnection, Path path, MediaType mediaType) {
    super(fsConnection, path, mediaType);
  }

  public FsBinaryDataPath(FsConnection fsConnection, DataPath dataPath) {
    super(fsConnection, dataPath);
  }


  @Override
  public FsBinaryFileManager getFileManager() {
    return FsBinaryFileManager.getSingleton();
  }

  @Override
  public Long getSize() {
    try {
      return Files.size(getAbsoluteNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Long getCount() {
    return null;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new UnsupportedOperationException("The insertion in binary file is not yet supported. File (" + getAbsoluteNioPath() + ")");
  }

  @Override
  public SelectStream getSelectStream() {
    throw new UnsupportedOperationException("The selection in binary file is not yet supported. File (" + getAbsoluteNioPath() + ")");
  }

}
